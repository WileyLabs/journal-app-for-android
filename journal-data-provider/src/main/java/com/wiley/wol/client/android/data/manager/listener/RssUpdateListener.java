package com.wiley.wol.client.android.data.manager.listener;

import com.wiley.wol.client.android.data.manager.Listener;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.domain.entity.FeedItemMO;
import com.wiley.wol.client.android.domain.entity.FeedMO;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.EventList;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.notification.ParamsBuilder;
import com.wiley.wol.client.android.utils.EncryptionUtils;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by taraskreknin on 03.10.14.
 */
public class RssUpdateListener implements Listener<InputStream> {

    private static final String TAG = RssUpdateListener.class.getSimpleName();

    private SimpleParser mSimpleParser;
    private NotificationCenter mNotificationCenter;
    private String mFeedUid;
    private HomePageService mHomePageService;

    public RssUpdateListener(FeedMO feed, NotificationCenter notificationCenter, HomePageService homePageService, SimpleParser simpleParser) {
        mSimpleParser = simpleParser;
        mHomePageService = homePageService;
        mNotificationCenter = notificationCenter;
        mFeedUid = feed.getUid();
    }

    @Override
    public void onComplete(InputStream result, final Object... additionalData) throws Exception {

        final String feedString = IOUtils.toString(result);

        FeedMO feed = mHomePageService.getFeed(mFeedUid);
        if (null == feed)
            return;

        List<FeedItemMO> feedItems = new ArrayList<>();
        RssFeed rssFeed = null;
        InputStream inputStream = null;
        try {
            inputStream = IOUtils.toInputStream(feedString);
            if (feedString.contains("<rdf:RDF")) {
                rssFeed = mSimpleParser.parse(inputStream, RssFeedRdf.class);
            } else {
                rssFeed = mSimpleParser.parse(inputStream, RssFeedClassic.class);
            }
        } catch (Exception ex) {
            throw new com.wiley.wol.client.android.error.ParseException(feedString, ex);
        } finally {
            if (null != inputStream)
                IOUtils.closeQuietly(inputStream);
        }

        final String rssTitle = rssFeed.getTitle();
        if (null == rssTitle || null == rssFeed.getList()) {
            throw new com.wiley.wol.client.android.error.ParseException(feedString);
        }
        feed.setTitle(rssTitle);

        List<ItemTag> rssItems = rssFeed.getList();
        Date now = new Date();
        for(ItemTag rssItem : rssItems) {
            FeedItemMO feedItem = new FeedItemMO();

            feedItem.setUid(EncryptionUtils.md5Hash(rssItem.getTitle() + rssItem.getLink() + feed.getUrl()));
            feedItem.setImageLink(rssItem.getImageLink());
            feedItem.setAuthor(rssItem.getAuthors());
            feedItem.setDescr(rssItem.getDescription());
            feedItem.setTitle(rssItem.getTitle());
            feedItem.setUrl(rssItem.getLink());
            feedItem.setPubDate(null == rssItem.getPublicationDate() ? now : rssItem.getPublicationDate());

            feedItem.setFeed(feed);
/*
            StringBuilder sb = new StringBuilder("\n");
            sb.append("\n***** title = ").append(feed.getTitle());
            if (null != feedItem.getUrl()) sb.append("\n***** url = ").append(feedItem.getUrl());
            sb.append("\n***** imageLink = ").append(null == feedItem.getImageLink() ? "" : feedItem.getImageLink());
            sb.append("\n***** encoded = ").append(null == rssItem.getEncoded() ? "" : rssItem.getEncoded());
            sb.append("\n***** description = ").append(null == feedItem.getDescr() ? "" : feedItem.getDescr());
            sb.append("\n***** author = ").append(null == feedItem.getAuthor() ? "" : feedItem.getAuthor());
            if (null != feedItem.getTitle()) sb.append("\n***** title = ").append(feedItem.getTitle());
            if (null != feedItem.getPubDate()) sb.append("\n***** pubDate = ").append(feedItem.getPubDate().toString());
            Log.d("RSS Reader Item : ", sb.toString());
*/
            feedItems.add(feedItem);
        }

        Collections.sort(feedItems, new Comparator<FeedItemMO>() {
            @Override
            public int compare(FeedItemMO one, FeedItemMO another) {
                Date oneDate = one.getPubDate();
                Date anotherDate = another.getPubDate();
                long diff = oneDate.getTime() - anotherDate.getTime();
                return diff > 0 ? -1 : diff < 0 ? 1 : 0;
            }
        });

        feed.setItems(feedItems);
        mHomePageService.saveFeed(feed);

        mNotificationCenter.sendNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(),
                new ParamsBuilder()
                        .withUid(mFeedUid)
                        .succeed(true)
                        .notModified(false)
                        .get());
    }

    @Override
    public void onNotModified() {
        mNotificationCenter.sendNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(),
                new ParamsBuilder()
                        .withUid(mFeedUid)
                        .succeed(true)
                        .notModified(true)
                        .get());
    }

    @Override
    public void onError(Exception ex) {
        Logger.s(TAG, ex);
        mNotificationCenter.sendNotification(EventList.RSS_FEED_UPDATE_COMPLETED.getEventName(),
                new ParamsBuilder()
                        .withUid(mFeedUid)
                        .succeed(false)
                        .withError(ex)
                        .get());
    }


    private interface RssFeed {
        String getTitle();
        List<ItemTag> getList();
        ImageTag getImage();
    }

    @NamespaceList({
            @Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom10")
    })
    @Root(name = "rss")
    private static class RssFeedClassic implements RssFeed {

        @Element(name="channel", required = false)
        private ChannelTag channel;

        public ChannelTag getChannel() {
            return channel;
        }

        public void setChannel(ChannelTag channel) {
            this.channel = channel;
        }

        @Override
        public String getTitle() {
            return null == channel ? null : channel.getTitle();
        }

        @Override
        public List<ItemTag> getList() {
            return null == channel ? null : channel.getList();
        }

        @Override
        public ImageTag getImage() {
            return null == channel ? null : channel.getImage();
        }
    }

    @NamespaceList({
            @Namespace(reference = "http://www.w3.org/1999/02/22-rdf-syntax-ns#", prefix = "rdf"),
            @Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom10")
    })
    @Root(name = "rdf")
    private static class RssFeedRdf implements RssFeed {

        @Element(name="channel", required = false)
        private ChannelTag channel;

        @ElementList(entry="item", inline = true, required = false)
        private List<ItemTag> list = new ArrayList<>();

        public ChannelTag getChannel() {
            return channel;
        }

        public void setChannel(ChannelTag channel) {
            this.channel = channel;
        }

        @Override
        public String getTitle() {
            return channel.getTitle();
        }

        @Override
        public List<ItemTag> getList() {
            return list;
        }

        @Override
        public ImageTag getImage() {
            return channel.getImage();
        }
    }

    @Root(name = "channel")
    private static class ChannelTag {

        @Element(name="title", required = false)
        private String title;

        @Element(name="description", required = false)
        private String description;

        @Element(name="language", required = false)
        private String language;

        @Element(name="lastBuildDate", required = false)
        private String lastBuildDate;

        @Element(name="copyright", required = false)
        private String copyright;

        @Element(name="docs", required = false)
        private String docs;

        @Element(name="image", required = false)
        private ImageTag image;

        @ElementList(entry="item", inline = true, required = false)
        private List<ItemTag> list = new ArrayList<>();


        @ElementList(entry="link", inline =true, required = false)
        private List<Atom10> atom10Link = new ArrayList<>();

        public List<Atom10> getAtom10Link() {
            return atom10Link;
        }

        public void setAtom10Link(List<Atom10> atom10Link) {
            this.atom10Link = atom10Link;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getLastBuildDate() {
            return lastBuildDate;
        }

        public void setLastBuildDate(String lastBuildDate) {
            this.lastBuildDate = lastBuildDate;
        }

        public String getCopyright() {
            return copyright;
        }

        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }

        public String getDocs() {
            return docs;
        }

        public void setDocs(String docs) {
            this.docs = docs;
        }

        public ImageTag getImage() {
            return image;
        }

        public void setImage(ImageTag image) {
            this.image = image;
        }

        public List<ItemTag> getList() {
            return list;
        }

        public void setList(List<ItemTag> list) {
            this.list = list;
        }
    }

    @Root(name="link")
    @Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom10")
    private static class Atom10 {
        @Attribute(required=false)
        private String rel;

        @Text(required=false)
        private String link;

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @Root(name = "image")
    private static class ImageTag {

        @Element(name="title", required = false)
        private String title;

        @Element(name="url", required = false)
        private String url;

        @Element(name="link", required = false)
        private String link;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }

    @Root(name = "item")
    private static class ItemTag {

        @Element(name="title", required = false)
        private String title;

        @ElementList(entry = "link", inline = true, required = false, empty = false)
        private List<String> link;

        @Element(name="guid", required = false)
        private String guid;

        @Element(name="pubDate", required = false)
        private String pubDate;

        @Element(name="date", required = false)
        @Namespace(reference = "http://purl.org/dc/elements/1.1/", prefix = "dc")
        private String date;

        @ElementList(entry="author", inline = true, required = false)
        private List<String> author = new ArrayList<>();

        @ElementList(entry="creator", inline = true, required = false)
        private List<String> creator = new ArrayList<>();

        @ElementList(entry="category", inline = true, required = false)
        private List<String> category = new ArrayList<>();

        @Element(name="description", required = false)
        private String description;

        @Element(name="encoded", required = false)
        @Namespace(reference = "http://purl.org/rss/1.0/modules/content/", prefix = "content")
        private String encoded;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLink() {
            return link.isEmpty() ? null : link.get(0);
        }

        public void setLink(List<String> link) {
            this.link = link;
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public String getPubDate() {
            return pubDate;
        }

        public void setPubDate(String pubDate) {
            this.pubDate = pubDate;
        }

        public List<String> getAuthor() {
            return author;
        }

        public void setAuthor(List<String> author) {
            this.author = author;
        }

        public String getAuthors() {
            StringBuilder sb = new StringBuilder("");
            if (author.size() > 0) {
                for (int i=0; i<author.size(); i++) {
                    sb.append(author.get(i));
                    if (i != author.size() - 1)
                        sb.append(", ");
                }
            } else if (creator.size() > 0) {
                for (int i=0; i<creator.size(); i++) {
                    sb.append(creator.get(i));
                    if (i != creator.size() - 1)
                        sb.append(", ");
                }
            }
            return sb.toString();
        }

        public List<String> getCategory() {
            return category;
        }

        public void setCategory(List<String> category) {
            this.category = category;
        }

        public String getDescription() {
            if (null != getEncoded() && !"".equals(getEncoded()))
                return getEncoded();

            return null == description ? "" : description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getCreator() {
            return creator;
        }

        public void setCreator(List<String> creator) {
            this.creator = creator;
        }

        public String getImageLink() {
            final String description = getDescription();
            if (null == description || description.equals(""))
                return null;

            String beginPhrase = "src=\"";
            int indexStart = description.indexOf(beginPhrase);
            if (indexStart >= 0) {
                final String endPhrase = "\"";
                int indexEnd = description.indexOf(endPhrase, indexStart + beginPhrase.length());
                if (indexEnd < 0)
                    return null;

                return description.substring(indexStart + beginPhrase.length(), indexEnd);
            }

            beginPhrase = "src='";
            indexStart = description.indexOf(beginPhrase);
            if (indexStart >= 0) {
                final String endPhrase = "'";
                int indexEnd = description.indexOf(endPhrase, indexStart + beginPhrase.length());
                if (indexEnd < 0)
                    return null;

                return description.substring(indexStart + beginPhrase.length(), indexEnd);
            }

            return null;
        }

        public String getEncoded() {
            return encoded;
        }

        public void setEncoded(String encoded) {
            this.encoded = encoded;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Date getPublicationDate() {
            String simpleFormat;
            String date;
            if (null != getPubDate()) {
                simpleFormat = "EEE, dd MMM yyyy hh:mm:ss";
                date = getPubDate();
            } else if (null != getDate()) {
                simpleFormat = "yyyy-MM-dd'T'hh:mm:ss'Z'";
                date = getDate();
            } else {
                return null;
            }

            Date publicationDate = null;
            final SimpleDateFormat format = new SimpleDateFormat(simpleFormat, Locale.US);
            try {
                publicationDate = format.parse(date);
            } catch (ParseException ex) {
                Logger.s(TAG, "SimpleDateFormat: invalidFormat[" + simpleFormat + "]" + date, ex);
            }
            return publicationDate;
        }
    }
}
