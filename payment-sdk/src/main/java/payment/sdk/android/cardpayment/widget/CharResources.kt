package payment.sdk.android.cardpayment.widget

import payment.sdk.android.sdk.R

internal class CharResources {

    companion object {

        internal fun getDrawableResource(char: Char): Int? =
                resourcesMap[char]

        internal fun isAllowedChar(chr: Char) =
                resourcesMap.containsKey(chr)

        private val resourcesMap = hashMapOf(
                '0' to R.drawable.digit_0,
                '1' to R.drawable.digit_1,
                '2' to R.drawable.digit_2,
                '3' to R.drawable.digit_3,
                '4' to R.drawable.digit_4,
                '5' to R.drawable.digit_5,
                '6' to R.drawable.digit_6,
                '7' to R.drawable.digit_7,
                '8' to R.drawable.digit_8,
                '9' to R.drawable.digit_9,
                'A' to R.drawable.letter_a,
                'B' to R.drawable.letter_b,
                'C' to R.drawable.letter_c,
                'D' to R.drawable.letter_d,
                'E' to R.drawable.letter_e,
                'F' to R.drawable.letter_f,
                'G' to R.drawable.letter_g,
                'H' to R.drawable.letter_h,
                'I' to R.drawable.letter_i,
                'J' to R.drawable.letter_j,
                'K' to R.drawable.letter_k,
                'L' to R.drawable.letter_l,
                'M' to R.drawable.letter_m,
                'N' to R.drawable.letter_n,
                'O' to R.drawable.letter_o,
                'P' to R.drawable.letter_p,
                'Q' to R.drawable.letter_q,
                'R' to R.drawable.letter_r,
                'S' to R.drawable.letter_s,
                'T' to R.drawable.letter_t,
                'U' to R.drawable.letter_u,
                'V' to R.drawable.letter_v,
                'W' to R.drawable.letter_w,
                'X' to R.drawable.letter_x,
                'Y' to R.drawable.letter_y,
                'Z' to R.drawable.letter_z,
                '-' to R.drawable.char_dash,
                '/' to R.drawable.char_slash,
                ' ' to R.drawable.char_space,
                '.' to R.drawable.char_period,
                '\'' to R.drawable.char_apostrophe
        )


    }
}