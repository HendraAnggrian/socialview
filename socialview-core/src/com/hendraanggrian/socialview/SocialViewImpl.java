package com.hendraanggrian.socialview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import com.hendraanggrian.support.utils.content.Themes;
import com.hendraanggrian.support.utils.text.Spannables;
import com.hendraanggrian.support.utils.util.Logs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hendraanggrian.socialview.R.styleable.SocialView;

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
public class SocialViewImpl implements SociableView, TextWatcher {

    private static final String TAG = "SocialView";
    private static boolean DEBUG;
    private static final int TYPE_HASHTAG = 1;
    private static final int TYPE_MENTION = 2;
    private static final int TYPE_HYPERLINK = 4;
    private static Pattern PATTERN_HASHTAG = Pattern.compile("(?i)[#＃]([0-9A-Z_À-ÖØ-öø-ÿ]*[A-Z_]+[a-z0-9_üÀ-ÖØ-öø-ÿ]*)");
    private static Pattern PATTERN_MENTION = Pattern.compile("(?i)@([0-9A-Z_À-ÖØ-öø-ÿ]*[A-Z_]+[a-z0-9_üÀ-ÖØ-öø-ÿ]*)");


    @NonNull private final TextView textView;
    private int enabledFlag;
    @ColorInt private int hashtagColor;
    @ColorInt private int mentionColor;
    @ColorInt private int hyperlinkColor;
    @Nullable private OnSocialClickListener hashtagListener;
    @Nullable private OnSocialClickListener mentionListener;
    @Nullable private SocialTextWatcher hashtagWatcher;
    @Nullable private SocialTextWatcher mentionWatcher;

    private boolean isHashtagEditing;
    private boolean isMentionEditing;

    public SocialViewImpl(@NonNull TextView textView, @NonNull Context context, @Nullable AttributeSet attrs) {
        this.textView = textView;
        this.textView.setText(textView.getText(), TextView.BufferType.SPANNABLE);
        this.textView.addTextChangedListener(this);
        TypedArray a = context.obtainStyledAttributes(attrs, SocialView, 0, 0);
        try {
            enabledFlag = a.getInteger(R.styleable.SocialView_socialEnabled, TYPE_HASHTAG | TYPE_MENTION | TYPE_HYPERLINK);
            int defaultColor = Themes.getColor(context, R.attr.colorAccent, textView.getCurrentTextColor());
            hashtagColor = a.getColor(R.styleable.SocialView_hashtagColor, defaultColor);
            mentionColor = a.getColor(R.styleable.SocialView_mentionColor, defaultColor);
            hyperlinkColor = a.getColor(R.styleable.SocialView_hyperlinkColor, defaultColor);
        } finally {
            a.recycle();
            colorize();
        }
    }

    @NonNull
    @Override
    public TextView getTextView() {
        return textView;
    }

    @Override
    public boolean isHashtagEnabled() {
        return (enabledFlag | TYPE_HASHTAG) == enabledFlag;
    }

    @Override
    public boolean isMentionEnabled() {
        return (enabledFlag | TYPE_MENTION) == enabledFlag;
    }

    @Override
    public boolean isHyperlinkEnabled() {
        return (enabledFlag | TYPE_HYPERLINK) == enabledFlag;
    }

    @Override
    public void setHashtagEnabled(boolean enabled) {
        enabledFlag = enabled
                ? enabledFlag | TYPE_HASHTAG
                : enabledFlag & (~TYPE_HASHTAG);
        colorize();
    }

    @Override
    public void setMentionEnabled(boolean enabled) {
        enabledFlag = enabled
                ? enabledFlag | TYPE_MENTION
                : enabledFlag & (~TYPE_MENTION);
        colorize();
    }

    @Override
    public void setHyperlinkEnabled(boolean enabled) {
        enabledFlag = enabled
                ? enabledFlag | TYPE_HYPERLINK
                : enabledFlag & (~TYPE_HYPERLINK);
        colorize();
    }

    @ColorInt
    @Override
    public int getHashtagColor() {
        return hashtagColor;
    }

    @ColorInt
    @Override
    public int getMentionColor() {
        return mentionColor;
    }

    @ColorInt
    @Override
    public int getHyperlinkColor() {
        return hyperlinkColor;
    }

    @Override
    public void setHashtagColor(@ColorInt int color) {
        hashtagColor = color;
        colorize();
    }

    @Override
    public void setMentionColor(@ColorInt int color) {
        mentionColor = color;
        colorize();
    }

    @Override
    public void setHyperlinkColor(@ColorInt int color) {
        hyperlinkColor = color;
        colorize();
    }

    @Override
    public void setHashtagColorRes(@ColorRes int colorRes) {
        setHashtagColor(ContextCompat.getColor(textView.getContext(), colorRes));
    }

    @Override
    public void setMentionColorRes(@ColorRes int colorRes) {
        setMentionColor(ContextCompat.getColor(textView.getContext(), colorRes));
    }

    @Override
    public void setHyperlinkColorRes(@ColorRes int colorRes) {
        setHyperlinkColor(ContextCompat.getColor(textView.getContext(), colorRes));
    }

    @Override
    public void setHashtagColorAttr(@AttrRes int colorAttr) {
        int color = Themes.getColor(textView.getContext(), colorAttr, 0);
        if (color == 0)
            throw new IllegalArgumentException("color attribute not found in current theme!");
        setHashtagColor(color);
    }

    @Override
    public void setMentionColorAttr(@AttrRes int colorAttr) {
        int color = Themes.getColor(textView.getContext(), colorAttr, 0);
        if (color == 0)
            throw new IllegalArgumentException("color attribute not found in current theme!");
        setMentionColor(color);
    }

    @Override
    public void setHyperlinkColorAttr(@AttrRes int colorAttr) {
        int color = Themes.getColor(textView.getContext(), colorAttr, 0);
        if (color == 0)
            throw new IllegalArgumentException("color attribute not found in current theme!");
        setHyperlinkColor(color);
    }

    @Override
    public void setOnHashtagClickListener(@Nullable OnSocialClickListener listener) {
        if (textView.getMovementMethod() == null || !(textView.getMovementMethod() instanceof LinkMovementMethod)) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        hashtagListener = listener;
        colorize();
    }

    @Override
    public void setOnMentionClickListener(@Nullable OnSocialClickListener listener) {
        if (textView.getMovementMethod() == null || !(textView.getMovementMethod() instanceof LinkMovementMethod)) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        mentionListener = listener;
        colorize();
    }

    @Override
    public void setHashtagTextChangedListener(@Nullable SocialTextWatcher watcher) {
        hashtagWatcher = watcher;
    }

    @Override
    public void setMentionTextChangedListener(@Nullable SocialTextWatcher watcher) {
        mentionWatcher = watcher;
    }

    @NonNull
    @Override
    public Collection<String> getHashtags() {
        if (!isHashtagEnabled()) {
            return Collections.emptyList();
        }
        return listOf(textView.getText(), PATTERN_HASHTAG);
    }

    @NonNull
    @Override
    public Collection<String> getMentions() {
        if (!isMentionEnabled()) {
            return Collections.emptyList();
        }
        return listOf(textView.getText(), PATTERN_MENTION);
    }

    @NonNull
    @Override
    public Collection<String> getHyperlinks() {
        if (!isHyperlinkEnabled()) {
            return Collections.emptyList();
        }
        return listOf(textView.getText(), Patterns.WEB_URL);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // triggered when text is backspaced
        if (DEBUG)
            Logs.d(TAG, "beforeTextChanged s=%s  start=%s    count=%s    after=%s", s, start, count, after);

        if (count > 0 && start > 0) {

            if (DEBUG)
                Log.d(TAG, "charAt " + String.valueOf(s.charAt(start - 1)));

            switch (s.charAt(start - 1)) {
                case '#':
                    isHashtagEditing = true;
                    isMentionEditing = false;
                    break;
                case '@':
                    isHashtagEditing = false;
                    isMentionEditing = true;
                    break;
                default:
                    if (!Character.isLetterOrDigit(s.charAt(start - 1))) {
                        isHashtagEditing = false;
                        isMentionEditing = false;
                    } else if (hashtagWatcher != null && isHashtagEditing) {
                        hashtagWatcher.onSocialTextChanged(SocialViewImpl.this, s.subSequence(indexOfPreviousNonLetterDigit(s, 0, start - 1) + 1, start).toString());
                    } else if (mentionWatcher != null && isMentionEditing) {
                        mentionWatcher.onSocialTextChanged(SocialViewImpl.this, s.subSequence(indexOfPreviousNonLetterDigit(s, 0, start - 1) + 1, start).toString());
                    }
                    break;
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (DEBUG)
            Logs.d(TAG, "onSocialTextChanged s=%s  start=%s    before=%s   count=%s", s, start, before, count);

        if (s.length() > 0) {
            colorize();

            // triggered when text is added
            if (start < s.length()) {

                if (DEBUG)
                    Log.d(TAG, "charAt " + String.valueOf(s.charAt(start + count - 1)));

                switch (s.charAt(start + count - 1)) {
                    case '#':
                        isHashtagEditing = true;
                        isMentionEditing = false;
                        break;
                    case '@':
                        isHashtagEditing = false;
                        isMentionEditing = true;
                        break;
                    default:
                        if (!Character.isLetterOrDigit(s.charAt(start))) {
                            isHashtagEditing = false;
                            isMentionEditing = false;
                        } else if (hashtagWatcher != null && isHashtagEditing) {
                            hashtagWatcher.onSocialTextChanged(SocialViewImpl.this, s.subSequence(indexOfPreviousNonLetterDigit(s, 0, start) + 1, start + count).toString());
                        } else if (mentionWatcher != null && isMentionEditing) {
                            mentionWatcher.onSocialTextChanged(SocialViewImpl.this, s.subSequence(indexOfPreviousNonLetterDigit(s, 0, start) + 1, start + count).toString());
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private int indexOfNextNonLetterDigit(CharSequence text, int start) {
        for (int i = start + 1; i < text.length(); i++) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                return i;
            }
        }
        return text.length();
    }

    private int indexOfPreviousNonLetterDigit(CharSequence text, int start, int end) {
        for (int i = end; i > start; i--) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                return i;
            }
        }
        return start;
    }

    private void colorize() {
        CharSequence text = textView.getText();
        if (!(text instanceof Spannable)) {
            throw new IllegalStateException("Attached text is not a Spannable, add TextView.BufferType.SPANNABLE when setting text to this TextView.");
        }
        final Spannable spannable = (Spannable) text;
        // remove all spans
        for (CharacterStyle span : spannable.getSpans(0, spannable.length(), CharacterStyle.class)) {
            spannable.removeSpan(span);
        }
        // refill new spans
        if (isHashtagEnabled()) {
            Spannables.putSpansAll(spannable, PATTERN_HASHTAG, new Spannables.SpanGetter() {
                @NonNull
                @Override
                public Object getSpan() {
                    if (hashtagListener == null) {
                        return new ForegroundColorSpan(hashtagColor);
                    }
                    return new ForegroundColorClickableSpan(hashtagColor) {
                        @Override
                        public void onClick(View widget) {
                            hashtagListener.onSocialClick(SocialViewImpl.this, spannable.subSequence(spannable.getSpanStart(this) + 1, spannable.getSpanEnd(this)));
                        }
                    };
                }
            });
        }
        if (isMentionEnabled()) {
            Spannables.putSpansAll(spannable, PATTERN_MENTION, new Spannables.SpanGetter() {
                @NonNull
                @Override
                public Object getSpan() {
                    if (mentionListener == null) {
                        return new ForegroundColorSpan(mentionColor);
                    }
                    return new ForegroundColorClickableSpan(mentionColor) {
                        @Override
                        public void onClick(View widget) {
                            mentionListener.onSocialClick(SocialViewImpl.this, spannable.subSequence(spannable.getSpanStart(this) + 1, spannable.getSpanEnd(this)));
                        }
                    };
                }
            });
        }
        if (isHyperlinkEnabled()) {
            Spannables.putSpansAll(spannable, Patterns.WEB_URL, new Spannables.SpanGetter() {
                @NonNull
                @Override
                public Object getSpan() {
                    return new SimpleURLSpan(spannable, hyperlinkColor);
                }
            });
        }
    }

    @NonNull
    static List<String> listOf(@NonNull CharSequence input, @NonNull Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group(pattern != Patterns.WEB_URL
                    ? 1 // remove hashtag and mention symbol
                    : 0));
        }
        return list;
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static void setHashtagPattern(@NonNull String regex) {
        SocialViewImpl.PATTERN_HASHTAG = Pattern.compile(regex);
    }

    public static void setMentionPattern(@NonNull String regex) {
        SocialViewImpl.PATTERN_MENTION = Pattern.compile(regex);
    }

    /**
     * Attach SocialView to any TextView or its subclasses.
     */
    @NonNull
    public static SociableView attach(@NonNull final TextView textView) {
        return new SocialViewImpl(textView, textView.getContext(), null);
    }
}