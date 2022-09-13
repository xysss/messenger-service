package com.example.messengerservicedemo.util

import com.example.messengerservicedemo.ext.logFlag
import me.hgj.mvvmhelper.ext.logE
import java.io.File

/**
 * 作者 : xys
 * 时间 : 2022-09-13 17:03
 * 描述 : 描述
 */
object FileUtils {

    /**
     * 遍历删除指定文件或文件夹下面的文件
     */
    fun deleteDirectoryFiles(directory: File?) {
        if (directory == null || !directory.exists() || !directory.isDirectory) {
            "deleteFileByDirectory directory is null".logE(logFlag)
            return
        }
        if (directory.exists() && directory.isDirectory) {
            for (listFile in directory.listFiles()) {
                if (listFile.isFile) {
                    listFile.delete()
                } else if (listFile.isDirectory) {
                    deleteDirectoryFiles(listFile)
                }
            }
        }
    }
}