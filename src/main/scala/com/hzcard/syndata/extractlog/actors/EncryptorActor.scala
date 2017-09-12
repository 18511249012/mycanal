package com.hzcard.syndata.extractlog.actors

import java.nio.charset.Charset
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import akka.actor.Actor
import com.hzcard.syndata.config.autoconfig.Encryptor
import org.slf4j.LoggerFactory
import spray.json._

object EncryptorActor {
  case class Plaintext(message: JsObject)
  case class Ciphertext(message: JsObject)
}


class EncryptorActor (
                        config: Encryptor
                      ) extends Actor {
  import EncryptorActor._

  protected val log = LoggerFactory.getLogger(getClass)

  private val charset = Charset.forName("UTF-8")
  private val decoder = Base64.getDecoder
  private val encoder = Base64.getEncoder

  private val cipher = config.getCipher
  private val decodedKey = decoder.decode(config.getKey)
  private val originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, cipher)
  private val encryptEngine = Cipher.getInstance(cipher)
  private val decryptEngine = Cipher.getInstance(cipher)

  private val encryptFields = config.getEncryptFields.toLowerCase().split(',').map(_.trim)

  override def preStart() = {
    encryptEngine.init(Cipher.ENCRYPT_MODE, originalKey)
    decryptEngine.init(Cipher.DECRYPT_MODE, originalKey)
  }

  def receive = {
    case Plaintext(message) =>
      val result = encryptFields(message)
      sender() ! result

    case Ciphertext(message) =>
      try {
        val result = decryptFields(message)
        sender() ! result
      }
      catch {
        case e:Exception => sender() ! akka.actor.Status.Failure(e)
      }
  }

  private def encryptFields(message: JsObject, jsonPath: String = ""): JsObject = {
    JsObject(
      message.fields.map({
        case (k:String, plaintextValue:JsValue) if encryptFields.contains(getNextJsonPath(jsonPath, k)) =>
          val plaintextBytes = plaintextValue.compactPrint.getBytes(charset)
          val cipherText = encryptEngine.doFinal(plaintextBytes)
          val v = encoder.encodeToString(cipherText)
          k -> JsString(v)
        case (k:String, jsObj: JsObject) =>
          k -> encryptFields(jsObj, getNextJsonPath(jsonPath, k))
        case (k:String, v: JsValue) =>
          k -> v
      })
    )
  }

  private def decryptFields(message: JsObject, jsonPath: String = ""): JsObject = {
    JsObject(
      message.fields.map({
        case (k:String, JsString(ciphertextValue)) if encryptFields.contains(getNextJsonPath(jsonPath, k)) =>
          val ciphertextBytes = decoder.decode(ciphertextValue)
          val plaintextBytes = decryptEngine.doFinal(ciphertextBytes)
          val v = new String(plaintextBytes, charset).parseJson
          k -> v
        case (k:String, jsObj: JsObject) =>
          k -> decryptFields(jsObj, getNextJsonPath(jsonPath, k))
        case (k:String, v: JsValue) =>
          k -> v
      })
    )
  }

  private def getNextJsonPath(jsonPath: String, nextPath: String): String = {
    Seq(jsonPath, nextPath).filter(_.nonEmpty).mkString(".")
  }
}
