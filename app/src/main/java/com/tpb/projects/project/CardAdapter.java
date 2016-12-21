package com.tpb.projects.project;

import android.content.ClipData;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.commonsware.cwac.anddown.AndDown;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 20/12/16.
 */

class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> {
    private static final String TAG = CardAdapter.class.getSimpleName();

    private ArrayList<Card> mCards = new ArrayList<>();
    private AndDown md = new AndDown();
    private ColumnFragment mParent;

    CardAdapter(ColumnFragment parent) {
        mParent = parent;
    }

    void setCards(ArrayList<Card> cards) {
        mCards = cards;
        notifyDataSetChanged();
    }

    ArrayList<Card> getCards() {
        return mCards;
    }

    @Override
    public CardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CardHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_card, parent, false));
    }

    @Override
    public void onBindViewHolder(CardHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        if(mCards.get(pos).requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
            mParent.loadIssue(new Loader.IssueLoader() {
                @Override
                public void issueLoaded(Issue issue) {
                    mCards.get(pos).setRequiresLoadingFromIssue(false);
                    mCards.get(pos).setNote(issue.getTitle());
                    holder.mSpinner.setVisibility(View.INVISIBLE);
                    notifyItemChanged(pos);
                }

                @Override
                public void loadError() {

                }
            }, mCards.get(pos).getIssueId());
        } else {
            holder.mMarkDown.setHtml(
                    md.markdownToHtml(
                            mCards.get(holder.getAdapterPosition()).getNote()
                    ),
                    new HtmlHttpImageGetter(holder.mMarkDown)
            );
        }
        holder.mCardView.setTag(pos);
        holder.mCardView.setOnLongClickListener(view -> {
            final ClipData data = ClipData.newPlainText("", "");
            final View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDrag(data, shadowBuilder, view, 0);
            view.setVisibility(View.INVISIBLE);
            return true;
        });
        holder.mCardView.setOnDragListener(new DragListener());

    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    class CardHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card_markdown) HtmlTextView mMarkDown;
        @BindView(R.id.card_issue_progress) ProgressBar mSpinner;
        @BindView(R.id.viewholder_card) CardView mCardView;

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }
}
