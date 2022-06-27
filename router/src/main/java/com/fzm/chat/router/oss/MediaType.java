package com.fzm.chat.router.oss;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.fzm.chat.router.oss.MediaType.*;

/**
 * @author zhengjy
 * @since 2021/01/28
 * Description:
 */
@IntDef(value = {AUDIO, PICTURE, VIDEO, FILE})
@Retention(RetentionPolicy.SOURCE)
public @interface MediaType {
    int AUDIO = 0;

    int PICTURE = 1;

    int VIDEO = 2;

    int FILE = 3;
}
