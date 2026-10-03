package org.example.project.data.remote.parser

import java.io.File

class DeviceManagerFactory private constructor() {

    companion object {
        fun create(file: File): DeviceManager = when(file.extension){
            "csv" -> DeviceManagerCsv(file)
            "json" -> DeviceManagerJson(file)
            else -> DeviceManagerJson(file)
        }
    }
}
