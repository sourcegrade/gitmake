package org.sourcegrade.gitmake

import java.nio.file.Path

fun Path.runCmd(vararg parts: String): Process =
    ProcessBuilder(*parts)
        .directory(toFile())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

fun Path.runCmdBlocking(vararg parts: String): Boolean = runCmd(*parts).waitFor() == 0

fun Path.runCmdThrowing(vararg parts: String) = check(runCmdBlocking(*parts)) { "Process failed" }
