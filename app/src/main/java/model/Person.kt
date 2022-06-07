package model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 作者 : xys
 * 时间 : 2022-06-06 14:37
 * 描述 : 描述
 */

@Parcelize
data class Person(
    var name: String,
    var age: Int) : Parcelable