package io.github.sor2171.superframevision.core.utils

import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileHashUtilsTest {

    private val fs = FileSystem.SYSTEM
    private val testDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sfv_test_${Random.nextLong()}"

    @Test
    fun testSameSmallFile() {
        fs.createDirectories(testDir)
        try {
            val fileA = testDir / "a.bin"
            val fileB = testDir / "b.bin"
            val content = "Hello SuperFrameVision Test Content!"

            fs.write(fileA) { writeUtf8(content) }
            fs.write(fileB) { writeUtf8(content) }

            assertTrue(isSameFile(fileA, fileB, fileSystem = fs))
        } finally {
            fs.deleteRecursively(testDir)
        }
    }

    @Test
    fun testDifferentSizeReturnsFalse() {
        fs.createDirectories(testDir)
        try {
            val fileA = testDir / "a.bin"
            val fileB = testDir / "b.bin"

            fs.write(fileA) { writeUtf8("Content 1") }
            fs.write(fileB) { writeUtf8("Content 12345") }

            assertFalse(isSameFile(fileA, fileB, fileSystem = fs))
        } finally {
            fs.deleteRecursively(testDir)
        }
    }

    @Test
    fun testSameSizeDifferentContentReturnsFalse() {
        fs.createDirectories(testDir)
        try {
            val fileA = testDir / "a.bin"
            val fileB = testDir / "b.bin"

            fs.write(fileA) { writeUtf8("Content A") }
            fs.write(fileB) { writeUtf8("Content B") }

            assertEquals(fs.metadataOrNull(fileA)?.size, fs.metadataOrNull(fileB)?.size)
            assertFalse(isSameFile(fileA, fileB, fileSystem = fs))
        } finally {
            fs.deleteRecursively(testDir)
        }
    }

    @Test
    fun testLargeFileHeadTailHash() {
        fs.createDirectories(testDir)
        try {
            val fileA = testDir / "largeA.bin"
            val fileB = testDir / "largeB.bin"
            val fileC = testDir / "largeC.bin"

            // 150 KB test data
            val head = ByteArray(64 * 1024) { (it % 256).toByte() }
            val middleA = ByteArray(22 * 1024) { 1 }
            val middleB = ByteArray(22 * 1024) { 2 } // different middle, but same head and tail
            val tail = ByteArray(64 * 1024) { ((it + 42) % 256).toByte() }

            // fileA has middleA
            fs.write(fileA) {
                write(head)
                write(middleA)
                write(tail)
            }

            // fileB has same head and tail, same size
            fs.write(fileB) {
                write(head)
                write(middleB)
                write(tail)
            }

            // fileC has different tail
            val differentTail = ByteArray(64 * 1024) { 99 }
            fs.write(fileC) {
                write(head)
                write(middleA)
                write(differentTail)
            }

            // fileA and fileB have same size, same head, same tail -> isSameFile should be true
            assertTrue(isSameFile(fileA, fileB, fileSystem = fs))

            // fileA and fileC have different tail -> isSameFile should be false
            assertFalse(isSameFile(fileA, fileC, fileSystem = fs))

            val hashA = getFileHeadTailHash(fileA, fileSystem = fs)
            val hashB = getFileHeadTailHash(fileB, fileSystem = fs)
            assertNotNull(hashA)
            assertEquals(hashA, hashB)
        } finally {
            fs.deleteRecursively(testDir)
        }
    }
}
