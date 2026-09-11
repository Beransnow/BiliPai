package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplyPageControlPolicyTest {

    @Test
    fun `default editor image state remains available`() {
        assertTrue(ReplyPageControl(uploadPictureIconState = 0).canUploadPicture)
        assertTrue(ReplyPageControl(uploadPictureIconState = 1).canUploadPicture)
    }

    @Test
    fun `explicitly disabled or hidden image action stays unavailable`() {
        assertFalse(ReplyPageControl(uploadPictureIconState = 2).canUploadPicture)
        assertFalse(ReplyPageControl(uploadPictureIconState = 3).canUploadPicture)
        assertFalse(
            ReplyPageControl(
                inputDisable = true,
                uploadPictureIconState = 1
            ).canUploadPicture
        )
    }
}
