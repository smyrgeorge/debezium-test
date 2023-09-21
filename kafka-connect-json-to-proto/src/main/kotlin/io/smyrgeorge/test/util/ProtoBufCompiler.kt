package io.smyrgeorge.test.util

import com.google.protobuf.DescriptorProtos
import java.io.File

object ProtoBufCompiler {

    private val tmp = "${System.getProperty("java.io.tmpdir").removeSuffix("/")}/protoc/kafka-connect"
    private const val protoc = "/opt/kafka-connect/protoc/bin/protoc"

    init {
        println("[ProtoFileCompiler]: $tmp")
        File(tmp).mkdirs()
    }

    fun compile(name: String, schema: String, compiler: String = protoc): DescriptorProtos.FileDescriptorProto {

        val protoFilePath = "$tmp/$name.proto"
        val descriptorFilePath = "$tmp/$name.desc"

        writeProto(protoFilePath, schema)

        val process = ProcessBuilder(
            protoc,
            "--proto_path=$tmp",
            "--descriptor_set_out=$descriptorFilePath",
            protoFilePath
        ).redirectErrorStream(true).start()

        val exitCode = process.waitFor()
        if (exitCode == 0) {
            println("[ProtoFileCompiler]: Proto file compiled successfully.")
        } else {
            error("[ProtoFileCompiler]: Failed to compile proto file.")
        }

        return readDescriptor(descriptorFilePath)
    }

    private fun writeProto(protoFilePath: String, schema: String) {
        File(protoFilePath).writeText(schema)
        println("[ProtoFileCompiler] $protoFilePath written to disk.")
    }

    private fun readDescriptor(descriptorFilePath: String): DescriptorProtos.FileDescriptorProto {
        val bytes = File(descriptorFilePath).readBytes()
        return DescriptorProtos.FileDescriptorProto.parseFrom(bytes)
    }
}