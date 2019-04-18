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
package com.wiley.android.journalApp.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.base.JournalActivity;
import com.wiley.android.journalApp.components.CustomWebView;
import com.wiley.android.journalApp.components.popup.PopupHost;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.dialog.SimpleSelectStringDialog;
import com.wiley.android.journalApp.fragment.popups.PopupSelectLink;
import com.wiley.android.journalApp.html.Templates;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.IosUtils;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.CitationUtils;
import com.wiley.wol.client.android.data.utils.GANHelper;
import com.wiley.wol.client.android.domain.entity.ArticleMO;
import com.wiley.wol.client.android.domain.entity.CitationMO;
import com.wiley.wol.client.android.domain.entity.ReferenceMO;
import com.wiley.wol.client.android.journalApp.theme.Theme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;

public class ReferencesAdapter extends BaseAdapter {

    public interface Host {
        PopupHost getPopupHost();

        ListView getListView();
    }

    private Templates templates = new Templates();
    @Inject
    private AANHelper aanHelper;
    @Inject
    private Theme theme = null;
    @Inject
    private WebController webController = null;
    private JournalActivity activity = null;
    private Context context = null;
    private Host host = null;
    protected final List<ReferenceMO> references = new ArrayList<>();
    private CustomWebView webView = null;

    public ReferencesAdapter(JournalActivity activity, Host host) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.host = host;
        final RoboInjector injector = RoboGuice.getInjector(this.context);
        injector.injectMembersWithoutViews(this);
    }

    public Collection<ReferenceMO> getReferences() {
        return this.references;
    }

    public void setReferences(Collection<ReferenceMO> newReferences) {
        notifyDataSetInvalidated();
        synchronized (references) {
            this.references.clear();
            if (newReferences != null) {
                this.references.addAll(newReferences);
            }
        }

        loadCitationsFromLazyForeignCollection();

        notifyDataSetChanged();
    }

    private void loadCitationsFromLazyForeignCollection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (references) {
                    for (ReferenceMO reference : references) {
                        final List<CitationMO> citations = new ArrayList<>(reference.getCitationSorted());
                        reference.setCitations(citations);
                    }
                }
            }
        }).start();
    }

    @Override
    public int getCount() {
        if (references.isEmpty()) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public Object getItem(int position) {
        return references;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.slider_panel_info_layout, parent, false);
        } else {
            view = convertView;
        }

        webView = (CustomWebView) view.findViewById(R.id.slider_panel_info_web_view);
        webView.setWebViewClient(webClient);

        setupWebView(webView);
        return view;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        loaded = false;
    }

    private void setupWebView(final CustomWebView webView) {
        loaded = false;

        StringBuilder refsHtml = new StringBuilder();

        int ri = 0;
        boolean prevEmpty = true;
        ReferenceMO prevRef = null;
        for (ReferenceMO ref : references) {
            ArticleMO article = ref.getArticle();
            Collection<CitationMO> cits = ref.getCitations();
            StringBuilder citsHtml = null;

            int ci = 0;
            for (CitationMO cit : cits) {
                if ((cit.getText() == null || cit.getText().trim().length() == 0)
                        && cit.getLinks() == null
                        && TextUtils.isEmpty(cit.getLinkToWOL())) {
                    continue;
                }

                if (citsHtml == null) {
                    citsHtml = new StringBuilder();
                }
                String text = cit.getText().replace("imageref:", article.getLocalPath("/").getAbsolutePath());
                String buttonTitle = !TextUtils.isEmpty(cit.getLinkToWOL())
                        ? (DeviceUtils.isTablet(context)
                        ? "Open on <span style='color:#007e8b;'>Wiley Online Library</span>"
                        : "Open on Wiley Online Library")
                        : "Open on...";

                String citHtml = templates.useAssetsTemplate(context, "cit_item")
                        .putParam("citation_text", text)
                        .putParam("open_on_button_display", cit.getLinks() == null ? "none" : "block")
                        .putParam("open_on_button_title", buttonTitle)
                        .putParam("citation_index", cit.getSortIndex())
                        .putParam("reference_index", ref.getSortIndex())
                        .putParam("citation_arr_index", ci)
                        .putParam("reference_arr_index", ri)
                        .proceed();

                citsHtml.append(citHtml);
                ci++;
            }

            if (prevRef != null && ref.getTitle().startsWith(prevRef.getTitle())) {
                if (!prevEmpty) {
                    refsHtml.append(templates.useAssetsTemplate(context, "ref_sep").proceed());
                }
            } else {
                if (prevRef != null) {
                    refsHtml.append(templates.useAssetsTemplate(context, "ref_cell_end").proceed());
                }
                refsHtml.append(
                        templates.useAssetsTemplate(context, "ref_cell_begin")
                                .putParam("reference_uid", ref.getId())
                                .proceed());
                prevRef = ref;
            }

            if (citsHtml != null) {
                boolean showLabel = !theme.isJournalHasNoReferenceNumbers();
                String refHtml = templates.useAssetsTemplate(context, "ref_item")
                        .putParam("ref_label", ref.getTitle())
                        .putParam("ref_label_display", showLabel ? "table-cell" : "none")
                        .putParam("citations", citsHtml.toString())
                        .putParam("reference_index", ref.getSortIndex())
                        .putParam("reference_uid", ref.getId())
                        .proceed();
                refsHtml.append(refHtml);
            }
            ri++;
            prevEmpty = citsHtml == null;
        }

        refsHtml.append(templates.useAssetsTemplate(context, "ref_cell_end").proceed());
        String html = templates.useAssetsTemplate(context, "refs")
                .putParam("ref_items", refsHtml.toString())
                .proceed();

        webView.loadData(html);
    }

    private void openCitation(int referenceIndex, int citationIndex) {
        ReferenceMO reference = references.get(referenceIndex);
        List<CitationMO> citations = reference.getCitationSorted();
        CitationMO citation = citations.get(citationIndex);

        if (!TextUtils.isEmpty(citation.getLinkToWOL())) {
            {
                aanHelper.trackActionOpenWebViewerForReference(citation.getLinkToWOL());
            }
            webController.openUrlInternal(citation.getLinkToWOL());
            GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                    GANHelper.ACTION_REFERENCE,
                    GANHelper.LABEL_WOL,
                    0L);
            return;
        }
        GANHelper.trackEvent(GANHelper.EVENT_ARTICLE,
                GANHelper.ACTION_REFERENCE,
                GANHelper.LABEL_LINK,
                0L);

        Map<String, Object> links = citation.getLinksMap();
        List<String> titles = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        for (CitationUtils.LinkType citationLinkType : CitationUtils.LinkType.values()) {
            String citationLinkAbbr = CitationUtils.getLinkString(citationLinkType);
            if (links.containsKey(citationLinkAbbr)) {
                titles.add(CitationUtils.getLinkTitle(citationLinkType));
                String citationLinkId;
                if (citationLinkType == CitationUtils.LinkType.ISI) {
                    citationLinkId = ((Map<String, String>) links.get(citationLinkAbbr)).get("ISI_ID");
                } else {
                    citationLinkId = (String) links.get(citationLinkAbbr);
                }
                String url = String.format("http://onlinelibrary.wiley.com/resolve/reference/%s?id=%s", citationLinkAbbr, citationLinkId);
                urls.add(url);
            }
        }

        if (DeviceUtils.isTablet(context)) {
            openCitationInPopup(reference, citation, titles, urls);
        } else {
            openCitationInDialog(reference, citation, titles, urls);
        }
    }

    private void openCitationInPopup(ReferenceMO reference, CitationMO citation, final List<String> titles, final List<String> urls) {
        webView.executeJavaScriptAndGetResult(
                String.format("return getElementAbsRect('openOnButton%d_%d');", reference.getSortIndex(), citation.getSortIndex()),
                new CustomWebView.JavaScriptExecutionCallback() {
                    @Override
                    public void onJavaScriptResult(String result) {
                        Rect rect = IosUtils.parseIosRectFromString(result);
                        if (rect == null) {
                            return;
                        }
                        rect = UIUtils.dpToPxRect(context, rect);
                        int[] loc = new int[2];
                        webView.getLocationInWindow(loc);
                        rect.offset(loc[0], loc[1]);
                        openCitationInPopupWithButtonRect(rect, titles, urls);
                    }
                });
    }

    private void openCitationInPopupWithButtonRect(Rect rect, List<String> titles, List<String> urls) {
        Point point = new Point(rect.right, rect.top + rect.height() / 2);
        PopupHost popupHost = host.getPopupHost();
        Bundle args = PopupSelectLink.makeArguments(titles, urls);
        popupHost.showFragmentAtPoint(PopupSelectLink.class, activity.getSupportFragmentManager(), point, args, PopupHost.Orientation.Horizontal);
    }

    private void openCitationInDialog(ReferenceMO reference, CitationMO citation, final List<String> titles, final List<String> urls) {
        SimpleSelectStringDialog.show(activity, "Open on...", titles.toArray(new String[titles.size()]), new SimpleSelectStringDialog.Listener() {

            @Override
            public void onDialogStringSelected(int index) {
                String url = urls.get(index);
                webController.openUrlInternal(url);
                {
                    aanHelper.trackActionOpenWebViewerForReference(url);
                }
            }

            @Override
            public void onDialogCancel() {
            }
        });
    }

    private boolean loaded = false;

    private WebViewClient webClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("onready")) {
                loaded = true;
                if (bibForScrollAfterLoad != null) {
                    doScrollToBib(bibForScrollAfterLoad);
                    bibForScrollAfterLoad = null;
                }
            } else if (url.startsWith("openon")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String[] parts = host.split("_");
                if (parts.length != 2) {
                    return false;
                }
                int referenceIndex = Integer.parseInt(parts[0]);
                int citationIndex = Integer.parseInt(parts[1]);
                openCitation(referenceIndex, citationIndex);
            } else if (url.startsWith("http")) {
                {
                    aanHelper.trackActionOpenWebViewerForReference(url);
                }
                webController.openUrlInternal(url);
            } else if (webController.canOpenUrlExternal(url)) {
                webController.openUrlExternal(url);
            } else {
                UIUtils.showLongToast(context, "Not implemented action: " + url);
            }
            return true;
        }
    };

    public void scrollToBib(String bib) {
        if (loaded) {
            doScrollToBib(bib);
        } else {
            bibForScrollAfterLoad = bib;
        }
    }

    private String bibForScrollAfterLoad = null;

    private void doScrollToBib(final String bib) {
        webView.executeJavaScriptAndGetResult(String.format("return getElementAbsRect('%s');", bib), new CustomWebView.JavaScriptExecutionCallback() {
            @Override
            public void onJavaScriptResult(String result) {
                Rect rect = IosUtils.parseIosRectFromString(result);
                if (rect == null) {
                    return;
                }
                rect = UIUtils.dpToPxRect(context, rect);
                ListView listView = host.getListView();
                int offset = rect.top + (rect.bottom - rect.top) / 2 - listView.getMeasuredHeight() / 2;
                listView.smoothScrollToPositionFromTop(0, -offset, 1000);
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.executeJavaScript(String.format("highlightElement('%s');", bib));
                    }
                }, 1000);
            }
        });
    }
}
