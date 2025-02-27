package cgeo.geocaching.ui;

import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;

import io.noties.markwon.Markwon;

/**
 * Encapsulates a text object to be set to a TextView.
 *
 * Supports setting this text from id or raw texts (including i18n parametrization) as well as various text formattings:
 * * markdown
 * * HTML
 * * Linkify
 * * accompanying icon/image
 *
 * Class is supposed to be used in parameters for View/Dialog helper methods dealing with text
 */
public class TextParam {

    @StringRes
    private final int textId;
    private final Object[] textParams;
    private final CharSequence text;


    private boolean useHtml = false;
    private boolean useMarkdown = false;
    private int linkifyMask = 0;

    private ImageParam image;
    private int imageSizeInDp = -1;


    /** create from text string resource id, optionally with parameters */
    public static TextParam id(@StringRes final int drawableId, final Object ... params) {
        return new TextParam(drawableId, null, params);
    }

    /** create from pure text, optionally with parameters */
    public static TextParam text(final CharSequence text, final Object ... params) {
        return new TextParam(0, text, params);
    }

    /** sets Linkify mask */
    public TextParam setLinkify(final int linkifyMask) {
        this.linkifyMask = linkifyMask;
        return this;
    }

    /** sets whether text shall be interpreted as markdown */
    public TextParam setMarkdown(final boolean useMarkdown) {
        this.useMarkdown = useMarkdown;
        return this;
    }

    /** sets whether text shall be interpreted as HTML */
    public TextParam setHtml(final boolean useHtml) {
        this.useHtml = useHtml;
        return this;
    }

    /** sets whether text shall be accompanied by an image/icon */
    public TextParam setImage(final ImageParam image) {
        return setImage(image, -1);
    }

        /** sets whether text shall be accompanied by an image/icon */
    public TextParam setImage(final ImageParam image, final int imageSizeInDp) {
        this.image = image;
        this.imageSizeInDp = imageSizeInDp;
        return this;
    }

    private TextParam(@StringRes final int textId, final CharSequence text, final Object ... params) {
        this.textId = textId;
        this.text = text;
        this.textParams = params;
    }

    /**
     * Applies the current settings of this TextParam to a textview.
     * * Sets text returned by {@link #getText(Context)}
     * * Calls {@link #adjust(TextView)} on the textview
     */
    public void applyTo(@Nullable final TextView view) {
        if (view == null) {
            return;
        }
        final CharSequence tcs = getText(view.getContext());
        if (tcs != null) {
            view.setText(tcs);
            adjust(view);
        }
    }

    /** creates text (CharSequence) to assign to a TextView according to this TextParam settings */
    public CharSequence getText(@Nullable final Context context) {
        CharSequence text;
        if (this.textId > 0 && context != null) {
            text = context.getResources().getText(this.textId);
        } else if (this.text != null) {
            text = this.text;
        } else {
            return null;
        }

        //parameters
        if (this.textParams != null && this.textParams.length > 0) {
            text = LocalizationUtils.getStringWithFallback(0, text.toString(), this.textParams);
        }

        //markdown
        if (this.useMarkdown && context != null) {
            final Markwon markwon = Markwon.create(context);
            text = markwon.toMarkdown(text.toString());
        }

        //html
        if (this.useHtml) {
            text = HtmlCompat.fromHtml(text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY);
        }

        //linkify
        if (this.linkifyMask != 0) {
            final SpannableString linkifyString = SpannableString.valueOf(text);
            Linkify.addLinks(linkifyString, this.linkifyMask);
            text = linkifyString;
        }

        return text;
    }

    /** Adjusts TextView properties other than the text itself so it conforms to this TextParam (e.g. MovementMethod) */
    public void adjust(final TextView view) {
        if (useHtml || linkifyMask != 0 || useMarkdown) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
        if (image != null || imageSizeInDp > 0) {
            final Drawable imageDrawable = (image == null ? ImageParam.id(android.R.color.transparent) : image)
                .getAsDrawable(view.getContext(), imageSizeInDp < 0 ? ViewUtils.pixelToDp(view.getTextSize() * 1.5f) : imageSizeInDp);
            if (imageSizeInDp < 0) {
                view.setCompoundDrawablesWithIntrinsicBounds(imageDrawable, null, null, null);
            } else {
                imageDrawable.setBounds(new Rect(0, 0, ViewUtils.dpToPixel(imageSizeInDp), ViewUtils.dpToPixel(imageSizeInDp)));
                view.setCompoundDrawables(imageDrawable, null, null, null);
            }

            //Add margin between image and text (support various screen densities)
            view.setCompoundDrawablePadding(ViewUtils.dpToPixel(10));
        }

    }

}
