/*  Journal App for Android
 *  Copyright (C) 2019 John Wiley & Sons, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wiley.android.journalApp.fragment.popups;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.fortysevendeg.swipelistview.SwipeListViewListener;
import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.activity.ArticleViewFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.components.ArticleComponent;
import com.wiley.android.journalApp.controller.ArticleController;
import com.wiley.android.journalApp.html.HtmlUtils;
import com.wiley.android.journalApp.utils.ArticleHolder;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.NotificationProcessor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wiley.wol.client.android.data.service.ArticleService.ARTICLE_MO;

/**
 * Created by taraskreknin on 17.06.14.
 */
public class PopupSavedArticlesEditor extends PopupFragment {

    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("MMMM dd, yyyy");

    @Inject
    private AANHelper aanHelper;
    @Inject
    private ArticleService mArticleService;
    @Inject
    private ArticleController mArticleController;
    @Inject
    private NotificationCenter mNotificationCenter;

    private SwipeListView mSwipeListView;
    private List<ArticleListItem> mItems = new ArrayList<>();
    private SavedIssuesAdapter mAdapter = new SavedIssuesAdapter();
    private View mListPlaceholder;
    private ViewGroup mPopupTitle;
    private TextView mFavoriteStatus;
    private ViewGroup mFavoriteStatusParent;
    private NotificationProcessor mFavStateProcessor = new NotificationProcessor() {
        @Override
        public void processNotification(Map<String, Object> params) {
            final ArticleMO article = (ArticleMO) params.get(ARTICLE_MO);
            onArticleRefFavoriteStateChanged(article);
        }
    };
    private SwipeListViewListener mSwipeListViewListener = new BaseSwipeListViewListener() {
        @Override
        public void onDismiss(int[] reverseSortedPositions) {
            int pos = reverseSortedPositions[0];
            onRemoveFromSaved(pos);
        }

        @Override
        public void onOpened(int position, boolean toRight) {
            mSwipeListView.closeOpenedItemsExcept(position);
        }

        @Override
        public void onClickFrontView(int position) {
            {
                aanHelper.trackActionGoToSavedArticle(mItems.get(position).mArticle.getDOI().getValue());
            }
            ((MainActivity) getActivity()).openArticles(getArticlesList(), mItems.get(position).mArticle.getDOI(),
                    getActivity().getString(R.string.title_saved_articles), true);
            hideSelf();
        }
    };

    private DataSetObserver mDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            onItemsChanged();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fillList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_saved_articles_popup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFavoriteStatusParent = findView(R.id.saved_articles_editor_fav_status_parent);
        mFavoriteStatusParent.setVisibility(getFavoriteStatusVisibility());

        mFavoriteStatus = findView(R.id.saved_articles_editor_fav_status);
        mFavoriteStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFavoriteStatus();
            }
        });

        mPopupTitle = findView(R.id.swipe_list_parent);
        mPopupTitle.setVisibility(mItems.isEmpty() && (mFavoriteStatusParent.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);

        mSwipeListView = findView(R.id.swipe_list_view);
        mSwipeListView.setAdapter(mAdapter);
        mSwipeListView.setSwipeListViewListener(mSwipeListViewListener);

        mListPlaceholder = findView(R.id.saved_articles_list_placeholder);
        onItemsChanged();
    }

    private List<DOI> getArticlesList() {
        final List<DOI> list = new ArrayList<>();
        for (ArticleListItem item : mItems) {
            list.add(item.mArticle.getDOI());
        }
        return list;
    }

    private int getFavoriteStatusVisibility() {
        ArticleMO currentArticle = getCurrentArticle();
        if (currentArticle == null) {
            return View.GONE;
        }
        return currentArticle.isFavorite() || isShowingSavedArticlesList() ? View.GONE : View.VISIBLE;
    }

    private void toggleFavoriteStatus() {
        getArticleComponent().toggleFavoriteState();
    }

    private ArticleMO getCurrentArticle() {
        if (getParentFragment() instanceof ArticleHolder) {
            return ((ArticleHolder) getParentFragment()).getCurrentArticle();
        } else {
            return null;
        }
    }

    private ArticleComponent getArticleComponent() {
        if (getParentFragment() instanceof ArticleHolder) {
            return ((ArticleHolder) getParentFragment()).getCurrentArticleComponent();
        } else {
            return null;
        }
    }

    private boolean isShowingSavedArticlesList() {
        return (getParentFragment() instanceof ArticleViewFragment) && ((ArticleViewFragment) getParentFragment()).isShowingSavedArticlesList();

    }

    private void onRemoveFromSaved(int pos) {
        ArticleListItem item = mItems.get(pos);
        mArticleService.removeArticleRefFromFavorites(item.mArticle);
        fillListAndNotify();
        mFavoriteStatusParent.setVisibility(getFavoriteStatusVisibility());
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.unregisterDataSetObserver(mDataObserver);
        mNotificationCenter.unSubscribeFromNotification(mFavStateProcessor);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.registerDataSetObserver(mDataObserver);
        mNotificationCenter.subscribeToNotification(EventList.ARTICLE_FAVORITE_STATE_CHANGED.getEventName(), mFavStateProcessor);
        fillListAndNotify();
    }

    public void onArticleRefFavoriteStateChanged(ArticleMO article) {
        if (mFavoriteStatusParent.getVisibility() != View.VISIBLE) {
            return;
        }

        boolean isFavorite = article.isFavorite();
        int statusTextResId = isFavorite ? R.string.saved : R.string.save_this_article;
        int iconResId = isFavorite ? R.drawable.favorite_highlighted_selector : R.drawable.favorite_normal_selector;
        mFavoriteStatus.setText(statusTextResId);
        mFavoriteStatus.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
        fillListAndNotify();
    }

    private void onItemsChanged() {
        if (mItems.size() == 0) {
            mListPlaceholder.setVisibility(View.VISIBLE);
            mSwipeListView.setVisibility(View.GONE);
            mPopupTitle.setVisibility(mFavoriteStatusParent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        } else {
            mListPlaceholder.setVisibility(View.GONE);
            mSwipeListView.setVisibility(View.VISIBLE);
            mPopupTitle.setVisibility(View.VISIBLE);
        }
    }

    private void fillList() {
        mItems.clear();
        List<ArticleMO> articles = mArticleService.getSavedArticles();
        if (articles == null || articles.size() == 0) {
            return;
        }
        for (ArticleMO article : articles) {
            if (!mArticleService.isArticleRefFavoriteChangingInProgress(article.getDOI())) {
                mItems.add(new ArticleListItem(article));
            }
        }
    }

    private void fillListAndNotify() {
        mAdapter.notifyDataSetInvalidated();
        fillList();
        mAdapter.notifyDataSetChanged();
    }

    private class SavedIssuesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            return mItems.get(position).getView(position, convertView, viewGroup);
        }
    }

    private interface ListItem {
        View getView(int position, View convertView, ViewGroup parent);
    }

    private class ArticleListItem implements ListItem {
        private ArticleMO mArticle;
        private String mPrimaryText;
        private String mSecondaryText;
        private LayoutInflater mInflater;

        public ArticleListItem(ArticleMO article) {
            mArticle = article;
            mInflater = LayoutInflater.from(getActivity());
            buildPrimaryText();
            buildSecondaryText();
        }

        private void buildPrimaryText() {
            String category = mArticle.getTocHeading1();
            if (TextUtils.isEmpty(category)) {
                mPrimaryText = mArticle.getTitle();
            } else {
                mPrimaryText = category + " | " + HtmlUtils.stripHtml(mArticle.getTitle());
            }
        }

        private void buildSecondaryText() {
            if (mArticle.isEarlyView()) {
                mSecondaryText = getString(R.string.early_view);
                return;
            }
            String pubDate = mArticle.getPublicationDate() != null ? sDateFormat.format(mArticle.getPublicationDate()) : "";
            String volNum = mArticle.getIssueValue(ArticleMO.VOLUME_NUMBER);
            String issueNum = mArticle.getIssueValue(ArticleMO.ISSUE_NUMBER);

            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(volNum)) {
                sb.append("Volume ");
                sb.append(volNum);
            }
            if (!TextUtils.isEmpty(issueNum)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("Issue ");
                sb.append(issueNum);
            }
            if (!TextUtils.isEmpty(pubDate)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("First Published Online ");
                sb.append(pubDate);
            }
            mSecondaryText = sb.toString();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.saved_articles_list_item, parent, false);
            }

            final TextView primary = (TextView) view.findViewById(R.id.list_item_primary_text);
            final TextView secondary = (TextView) view.findViewById(R.id.list_item_secondary_text);

            primary.setText(mPrimaryText);
            if (TextUtils.isEmpty(mSecondaryText)) {
                secondary.setVisibility(View.GONE);
            } else {
                secondary.setText(mSecondaryText);
                secondary.setVisibility(View.VISIBLE);
            }

            final View delete = view.findViewById(R.id.list_item_delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSwipeListView.closeOpenedItems();
                    mSwipeListView.dismiss(position);
                }
            });

            return view;
        }
    }
}
