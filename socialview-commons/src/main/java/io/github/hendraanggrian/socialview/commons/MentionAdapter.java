package io.github.hendraanggrian.socialview.commons;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
public final class MentionAdapter extends SuggestionAdapter<Mentionable> {

    @NonNull private final Picasso picasso;
    @DrawableRes private final int defaultAvatar;

    public MentionAdapter(@NonNull Context context) {
        this(context, R.drawable.ic_placeholder_mention);
    }

    public MentionAdapter(@NonNull Context context, @DrawableRes int defaultAvatar) {
        super(context, R.layout.item_mention, R.id.textview_mention_username);
        this.picasso = Picasso.with(context);
        this.defaultAvatar = defaultAvatar;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mention, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Mentionable item = getItem(position);
        if (item != null) {
            holder.textViewUsername.setText(item.getUsername());

            if (item.getAvatar() == null)
                picasso.load(defaultAvatar)
                        .into(holder.imageView);
            else if (item.getAvatar() instanceof Integer)
                picasso.load((int) item.getAvatar())
                        .into(holder.imageView);
            else if (item.getAvatar() instanceof String)
                picasso.load((String) item.getAvatar())
                        .placeholder(defaultAvatar)
                        .error(defaultAvatar)
                        .into(holder.imageView);
            else
                throw new RuntimeException("Mentionable avatar can only be String url and int resource.");

            if (!TextUtils.isEmpty(item.getDisplayname())) {
                holder.textViewDisplayname.setText(item.getDisplayname());
                holder.textViewDisplayname.setVisibility(View.VISIBLE);
            } else {
                holder.textViewDisplayname.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    @NonNull
    @Override
    public SuggestionFilter initializeFilter() {
        return new SuggestionFilter() {
            @Override
            public String getString(Mentionable item) {
                return item.getUsername();
            }
        };
    }

    private static class ViewHolder {
        private final ImageView imageView;
        private final TextView textViewUsername, textViewDisplayname;

        private ViewHolder(@NonNull View view) {
            imageView = (ImageView) view.findViewById(R.id.imageview_mention_username);
            textViewUsername = (TextView) view.findViewById(R.id.textview_mention_username);
            textViewDisplayname = (TextView) view.findViewById(R.id.textview_mention_displayname);
        }
    }
}