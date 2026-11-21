package cn.nubia.gamecenter.settings.watermark;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/* loaded from: classes.dex */
public class WaterMarkWatcher implements TextWatcher {
    private EditText mEditText;
    private int maxLength;

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public WaterMarkWatcher(EditText editText, int i) {
        this.mEditText = editText;
        this.maxLength = i;
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        int wordCount = getWordCount(charSequence.toString());
        if (wordCount > this.maxLength) {
            int i4 = i3 + i;
            CharSequence subSequence = charSequence.subSequence(i, i4);
            String limitString = getLimitString(subSequence.toString(), this.maxLength - (wordCount - getWordCount(subSequence.toString())));
            if (i4 == charSequence.length()) {
                this.mEditText.setText(String.valueOf(((Object) charSequence.subSequence(0, i)) + limitString));
                EditText editText = this.mEditText;
                editText.setSelection(editText.getText().length());
            } else {
                this.mEditText.setText(String.valueOf(((Object) charSequence.subSequence(0, i)) + limitString + String.valueOf(charSequence.subSequence(i4, charSequence.length()))));
                this.mEditText.setSelection(i + limitString.length());
            }
        }
    }

    public int getWordCount(String str) {
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            int codePointAt = Character.codePointAt(str, i2);
            i = (codePointAt < 0 || codePointAt > 255) ? i + 2 : i + 1;
        }
        return i;
    }

    private String getLimitString(String str, int i) {
        StringBuilder sb = new StringBuilder();
        int i2 = 0;
        for (int i3 = 0; i3 < str.length(); i3++) {
            int codePointAt = Character.codePointAt(str, i3);
            if (codePointAt < 0 || codePointAt > 255) {
                i2 += 2;
                if (i2 <= i) {
                    sb.append(str.charAt(i3));
                } else {
                    return sb.toString();
                }
            } else if (i2 < i) {
                i2++;
                sb.append(str.charAt(i3));
            } else {
                return sb.toString();
            }
        }
        return sb.toString();
    }
}
