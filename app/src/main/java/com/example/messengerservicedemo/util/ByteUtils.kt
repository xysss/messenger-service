package com.example.messengerservicedemo.util

import android.util.Log
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.xor

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:09
 * 描述 : 描述
 */


object ByteUtils {

    const val FRAME_START: Byte = 0x55
    const val FRAME_END: Byte = 0x23.toByte()
    const val FRAME_FF: Byte = 0xFF.toByte()
    const val FRAME_00: Byte = 0x00

    const val Msg03: Byte = 0x03.toByte()
    const val Msg88: Byte = 0x88.toByte()
    const val Msg84: Byte = 0x84.toByte()
    const val Msg26: Byte = 0x26.toByte()
    const val Msg61: Byte = 0x61.toByte()
    const val Msg62: Byte = 0x62.toByte()
    const val Msg00: Byte = 0x00.toByte()

    fun secondToTimes(second: Int): String {
        var h = 0
        var d = 0
        var s = 0
        val temp = second % 3600
        if (second > 3600) {
            h = second / 3600
            if (temp != 0) {
                if (temp > 60) {
                    d = temp / 60
                    if (temp % 60 != 0) {
                        s = temp % 60
                    }
                } else {
                    s = temp
                }
            }
        } else {
            d = second / 60
            if (second % 60 != 0) {
                s = second % 60
            }
        }
        return h.toString() + "时" + d + "分" + s + "秒"
    }


    /**
     * Convert hex string to byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    fun hexStringToBytes(hexString: String): ByteArray {
        val mHexString = hexString.uppercase(Locale.getDefault())
        val length = mHexString.length / 2
        val hexChars = mHexString.toCharArray()
        val byteArrayResult = ByteArray(length)
        for (i in 0 until length) {
            val pos = i * 2
            byteArrayResult[i] = (charToByte(hexChars[pos]).toInt().shl(4) or charToByte(hexChars[pos + 1]).toInt()).toByte()
        }
        Log.d("TAG", "hexStringToBytes: "+ byteArrayResult.contentToString())
        return byteArrayResult
    }

    fun getNoMoreThanTwoDigits(number: Float): String {
        val format = DecimalFormat("0.###")
        //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.FLOOR
        return format.format(number)
    }

    fun getDateTime(times: Long): String {
        return SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(times)
    }

    /**
     * 亦或校验(BCC校验)
     *
     * @param datas
     * @return
     */
    fun getBCCResult(datas: ByteArray): Byte {
        var temp = datas[0]
        for (i in 1 until datas.size) {
            temp = temp xor datas[i]
        }
        return temp
    }

    fun byteArrayToHexString(byteArray: ByteArray): String {
        val sb = StringBuilder()
        for (i in byteArray.indices) {
            var hex = Integer.toHexString((byteArray[i]).toInt() and 0xFF)
            if(hex.length == 1){
                hex = "0$hex"
                sb.append(hex.uppercase(Locale.getDefault()))
            }
        }
        return sb.toString()
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private fun charToByte(c: Char): Byte = "0123456789ABCDEF".indexOf(c).toByte()

    //CRC校验
    fun getCrc16Str(arr_buff: ByteArray, little_endian: Boolean = true): ByteArray {
        val len = arr_buff.size
        // 预置 1 个 16 位的寄存器为十六进制FFFF, 称此寄存器为 CRC寄存器。
        var crc = 0xFFFF
        for (i in 0 until len) {
            crc = ((crc and 0xFF00) or (crc and 0x00FF) xor (arr_buff[i].toInt() and 0xFF))
            for (j in 0 until 8) {
                // 把 CRC 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位
                if ((crc and 0x0001) > 0) {
                    // 如果移出位为 1, CRC寄存器与多项式A001进行异或
                    crc = crc.shr(1)
                    crc = crc xor 0xA001
                } else {
                    // 如果移出位为 0,再次右移一位
                    crc = crc.shr(1)
                }
            }
        }
        val result = ByteArray(2)
        result[if(little_endian) 0 else 1] = (crc.shr(8) and 0xFF).toByte()
        result[if(little_endian) 1 else 0] = (crc and 0xFF).toByte()
        return result
    }
}