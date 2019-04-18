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
package com.wiley.android.journalApp.components;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.Journal;
import com.wiley.android.journalApp.base.JournalFragment;
import com.wiley.android.journalApp.base.MainActivity;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.journalApp.theme.Theme;

public class QuickLinkMenuComponent {
    private Animation showQuickLinkMenuAnimation;
    private Animation hideQuickLinkMenuAnimation;
    private View quickLinkMenuView;
    private final Theme theme;
    private final boolean isPhone;
    private final ArticleService articleService;

    @Inject
    public QuickLinkMenuComponent(final Context context, final Theme theme, final ArticleService articleService) {
        this.theme = theme;
        this.articleService = articleService;
        isPhone = DeviceUtils.isPhone(context);
        if (isPhone) {
            showQuickLinkMenuAnimation = AnimationUtils.loadAnimation(context, R.anim.popup_show);
            hideQuickLinkMenuAnimation = AnimationUtils.loadAnimation(context, R.anim.popup_hide);
        }
    }
    
    public void initQuickLink(final Activity activity, final JournalFragment currentFragment) {
        this.quickLinkMenuView = currentFragment.findView(R.id.quick_link_menu_view);
        quickLinkMenuView.setBackgroundColor(theme.getMainColor());
        quickLinkMenuView.setVisibility(View.GONE);

        if (currentFragment instanceof Journal) {
            setHomeButtonOnClickListener(getJournalHomeButtonOnClickListener(activity));
            setSavedArticlesButtonOnClickListener(getJournalSavedArticleOnClickListener(activity));
            setSearchButtonOnClickListener(getJournalSearchButtonOnClickListener(activity));
        } else {
            setHomeButtonOnClickListener(getSocietyHomeButtonOnClickListener(activity));
            setSavedArticlesButtonOnClickListener(getSocietySavedArticleOnClickListener(activity));
            setSearchButtonOnClickListener(getSocietySearchButtonOnClickListener(activity));
        }

        setSettingsButtonOnClickListener(getSettingsButtonOnClickListener(activity));

        if (!theme.isJournalHasDarkBackground()) {
            ((ImageView) quickLinkMenuView.findViewById(R.id.home_quick_link_menu_button)).setImageResource(R.drawable.home_quick_link_menu_dark);
            ((ImageView) quickLinkMenuView.findViewById(R.id.saved_articles_quick_link_menu_button)).setImageResource(R.drawable.saved_articles_quick_link_menu_dark);
            ((ImageView) quickLinkMenuView.findViewById(R.id.search_quick_link_menu_button)).setImageResource(R.drawable.search_quick_link_menu_dark);
            ((ImageView) quickLinkMenuView.findViewById(R.id.settings_quick_link_menu_button)).setImageResource(R.drawable.settings_quick_link_menu_dark);
            ((ImageView) quickLinkMenuView.findViewById(R.id.alert_quick_link_menu_button)).setImageResource(R.drawable.alert_quick_link_menu_dark);
        }
    }

    public void setHomeButtonOnClickListener(View.OnClickListener listener) {
        quickLinkMenuView.findViewById(R.id.home_quick_link_menu_button).setOnClickListener(listener);
    }

    public void setSavedArticlesButtonOnClickListener(View.OnClickListener listener) {
        quickLinkMenuView.findViewById(R.id.saved_articles_quick_link_menu_button).setOnClickListener(listener);
    }

    public void setSearchButtonOnClickListener(View.OnClickListener listener) {
        quickLinkMenuView.findViewById(R.id.search_quick_link_menu_button).setOnClickListener(listener);
    }

    public void setSettingsButtonOnClickListener(View.OnClickListener listener) {
        quickLinkMenuView.findViewById(R.id.settings_quick_link_menu_button).setOnClickListener(listener);
    }

    public void setAlertButtonOnClickListener(View.OnClickListener listener) {
        quickLinkMenuView.findViewById(R.id.alert_quick_link_menu_button).setOnClickListener(listener);
    }

    public void showQuickLinkMenu() {
        if (isPhone && (quickLinkMenuView.getVisibility() == View.GONE)) {
            quickLinkMenuView.setVisibility(View.VISIBLE);
            quickLinkMenuView.startAnimation(showQuickLinkMenuAnimation);
            quickLinkMenuView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideQuickLinkMenu();

                }
            }, 3000);
        }
    }

    public void hideQuickLinkMenu() {
        if (isPhone && (quickLinkMenuView.getVisibility() == View.VISIBLE)) {
            quickLinkMenuView.startAnimation(hideQuickLinkMenuAnimation);
            quickLinkMenuView.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener getSettingsButtonOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ((MainActivity) activity).navigateToJournalSettings();
            }
        };
    }

    private View.OnClickListener getJournalSearchButtonOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ((MainActivity) activity).navigateToJournalGlobalSearch();
            }
        };
    }

    private View.OnClickListener getSocietySearchButtonOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ((MainActivity) activity).navigateToSocietyGlobalSearch();
            }
        };
    }

    private View.OnClickListener getJournalSavedArticleOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ((MainActivity) activity).navigateToJournalSavedArticles();
            }
        };
    }

    private View.OnClickListener getSocietySavedArticleOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                ((MainActivity) activity).navigateToSocietyFavorites();
            }
        };
    }

    private View.OnClickListener getJournalHomeButtonOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (articleService.hasArticlesForEarlyView()) {
                    ((MainActivity) activity).navigateToJournalEarlyView();
                } else {
                    ((MainActivity) activity).navigateToJournalIssues();
                }
            }
        };
    }

    private View.OnClickListener getSocietyHomeButtonOnClickListener(final Activity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).navigateToSocietyHome();
            }
        };
    }
}
