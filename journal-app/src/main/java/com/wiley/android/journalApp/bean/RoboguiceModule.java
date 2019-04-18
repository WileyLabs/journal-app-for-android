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
package com.wiley.android.journalApp.bean;

import android.content.Context;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.wiley.android.journalApp.authorization.Authorizer;
import com.wiley.android.journalApp.components.QuickLinkMenuComponent;
import com.wiley.android.journalApp.controller.ArticleController;
import com.wiley.android.journalApp.controller.ConnectionController;
import com.wiley.android.journalApp.controller.DriveController;
import com.wiley.android.journalApp.controller.DriveControllerImpl;
import com.wiley.android.journalApp.controller.FeedsController;
import com.wiley.android.journalApp.controller.FigureViewerImageLoaderHelper;
import com.wiley.android.journalApp.controller.ImageLoaderHelper;
import com.wiley.android.journalApp.controller.VideoController;
import com.wiley.android.journalApp.controller.WebController;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.error.ErrorManagerImpl;
import com.wiley.android.journalApp.notification.NotificationCenterImpl;
import com.wiley.android.journalApp.utils.DeviceUtils;
import com.wiley.android.journalApp.utils.EmailSender;
import com.wiley.android.journalApp.utils.FileUtils;
import com.wiley.wol.client.android.data.JournalAppDatabase;
import com.wiley.wol.client.android.data.dao.ArticleDao;
import com.wiley.wol.client.android.data.dao.ArticleDaoImpl;
import com.wiley.wol.client.android.data.dao.ArticleSpecialSectionDao;
import com.wiley.wol.client.android.data.dao.ArticleSpecialSectionDaoImpl;
import com.wiley.wol.client.android.data.dao.IssueDao;
import com.wiley.wol.client.android.data.dao.IssueDaoImpl;
import com.wiley.wol.client.android.data.dao.TPSSiteDao;
import com.wiley.wol.client.android.data.dao.TPSSiteDaoImpl;
import com.wiley.wol.client.android.data.dao.filter.FilterFactory;
import com.wiley.wol.client.android.data.dao.filter.FilterFactoryImpl;
import com.wiley.wol.client.android.data.http.DocumentsDownloader;
import com.wiley.wol.client.android.data.http.DocumentsDownloaderImpl;
import com.wiley.wol.client.android.data.http.DownloadManager;
import com.wiley.wol.client.android.data.http.DownloadManagerImpl;
import com.wiley.wol.client.android.data.http.OperationManager;
import com.wiley.wol.client.android.data.http.OperationManagerImpl;
import com.wiley.wol.client.android.data.http.ResourceManager;
import com.wiley.wol.client.android.data.http.ResourceManagerImpl;
import com.wiley.wol.client.android.data.http.UpdateManager;
import com.wiley.wol.client.android.data.http.UpdateManagerImpl;
import com.wiley.wol.client.android.data.manager.AdvertisementManager;
import com.wiley.wol.client.android.data.manager.AdvertisementManagerImpl;
import com.wiley.wol.client.android.data.manager.FeedsInfo;
import com.wiley.wol.client.android.data.manager.ImportManager;
import com.wiley.wol.client.android.data.manager.ImportManagerImpl;
import com.wiley.wol.client.android.data.manager.RssUpdateListenerProvider;
import com.wiley.wol.client.android.data.manager.RssUpdateListenerProviderImpl;
import com.wiley.wol.client.android.data.manager.listener.AnnouncementFeedListener;
import com.wiley.wol.client.android.data.service.AnnouncementService;
import com.wiley.wol.client.android.data.service.AnnouncementServiceImpl;
import com.wiley.wol.client.android.data.service.ArticleService;
import com.wiley.wol.client.android.data.service.ArticleServiceImpl;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.data.service.AuthorizationServiceImpl;
import com.wiley.wol.client.android.data.service.HomePageService;
import com.wiley.wol.client.android.data.service.HomePageServiceImpl;
import com.wiley.wol.client.android.data.service.InAppBillingService;
import com.wiley.wol.client.android.data.service.InAppBillingServiceImpl;
import com.wiley.wol.client.android.data.service.IssueService;
import com.wiley.wol.client.android.data.service.IssueServiceImpl;
import com.wiley.wol.client.android.data.service.SpecialSectionService;
import com.wiley.wol.client.android.data.service.SpecialSectionServiceImpl;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.xml.ArticleRefSimpleParser;
import com.wiley.wol.client.android.data.xml.ArticleRefSimpleParserImpl;
import com.wiley.wol.client.android.data.xml.ArticleSimpleParser;
import com.wiley.wol.client.android.data.xml.ArticleSimpleParserImpl;
import com.wiley.wol.client.android.data.xml.IssueSimpleParser;
import com.wiley.wol.client.android.data.xml.IssueSimpleParserImpl;
import com.wiley.wol.client.android.data.xml.SimpleParser;
import com.wiley.wol.client.android.data.xml.SimpleParserImpl;
import com.wiley.wol.client.android.data.xml.SpecialSectionSimpleParser;
import com.wiley.wol.client.android.data.xml.SpecialSectionSimpleParserImpl;
import com.wiley.wol.client.android.data.xml.loader.EarlyViewFeedLoader;
import com.wiley.wol.client.android.data.xml.loader.asset.AssetBasedEarlyViewFeedLoaderImpl;
import com.wiley.wol.client.android.inject.InjectCachePath;
import com.wiley.wol.client.android.inject.InjectDatabasePath;
import com.wiley.wol.client.android.inject.InjectExternalCachePath;
import com.wiley.wol.client.android.journalApp.receiver.CustomBroadcastReceiver;
import com.wiley.wol.client.android.journalApp.receiver.CustomBroadcastReceiverImpl;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;
import com.wiley.wol.client.android.notification.NotificationCenter;
import com.wiley.wol.client.android.settings.Environment;
import com.wiley.wol.client.android.settings.LastModifiedManager;
import com.wiley.wol.client.android.settings.Settings;
import com.wiley.wol.client.android.settings.SettingsImpl;

import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.BillingImpl;

import java.io.File;

import static java.lang.String.format;

public class RoboguiceModule extends AbstractModule {
    @Override
    protected void configure() {
        Logger.d(RoboguiceModule.class.getSimpleName(), "Configuring module...");

        bind(String.class).annotatedWith(InjectExternalCachePath.class).toProvider(ExternalCachePathProvider.class);
        bind(String.class).annotatedWith(InjectCachePath.class).toProvider(CachePathProvider.class);
        bind(String.class).annotatedWith(InjectDatabasePath.class).toProvider(DatabasePathProvider.class);

        bind(OrmLiteSqliteOpenHelper.class).to(JournalAppDatabase.class).in(Singleton.class);

        // provider: data.dao.services
        bind(IssueService.class).to(IssueServiceImpl.class).in(Singleton.class);
        bind(ArticleService.class).to(ArticleServiceImpl.class).in(Singleton.class);
        bind(SpecialSectionService.class).to(SpecialSectionServiceImpl.class).in(Singleton.class);
        bind(AuthorizationService.class).to(AuthorizationServiceImpl.class).in(Singleton.class);
        bind(HomePageService.class).to(HomePageServiceImpl.class).in(Singleton.class);
        bind(AnnouncementService.class).to(AnnouncementServiceImpl.class).in(Singleton.class);

        bind(EarlyViewFeedLoader.class).to(AssetBasedEarlyViewFeedLoaderImpl.class).in(Singleton.class);
        bind(ArticleRefSimpleParser.class).to(ArticleRefSimpleParserImpl.class).in(Singleton.class);
        bind(IssueSimpleParser.class).to(IssueSimpleParserImpl.class).in(Singleton.class);
        bind(ArticleSimpleParser.class).to(ArticleSimpleParserImpl.class).in(Singleton.class);
        bind(SimpleParser.class).to(SimpleParserImpl.class).in(Singleton.class);
        bind(SpecialSectionSimpleParser.class).to(SpecialSectionSimpleParserImpl.class).in(Singleton.class);
        bind(ResourceManager.class).to(ResourceManagerImpl.class).in(Singleton.class);
        bind(Environment.class).in(Singleton.class);
        bind(IssueDao.class).to(IssueDaoImpl.class).in(Singleton.class);
        bind(ArticleDao.class).to(ArticleDaoImpl.class).in(Singleton.class);
        bind(ArticleSpecialSectionDao.class).to(ArticleSpecialSectionDaoImpl.class).in(Singleton.class);
        bind(TPSSiteDao.class).to(TPSSiteDaoImpl.class).in(Singleton.class);
        bind(FilterFactory.class).to(FilterFactoryImpl.class).in(Singleton.class);
        bind(ImportManager.class).to(ImportManagerImpl.class).in(Singleton.class);
        bind(NotificationCenter.class).to(NotificationCenterImpl.class).in(Singleton.class);
        bind(DownloadManager.class).to(DownloadManagerImpl.class).in(Singleton.class);
        bind(OperationManager.class).to(OperationManagerImpl.class).in(Singleton.class);
        bind(UpdateManager.class).to(UpdateManagerImpl.class).in(Singleton.class);
        bind(DocumentsDownloader.class).to(DocumentsDownloaderImpl.class).in(Singleton.class);
        bind(ErrorManager.class).to(ErrorManagerImpl.class).in(Singleton.class);
        bind(AdvertisementManager.class).to(AdvertisementManagerImpl.class).in(Singleton.class);
        bind(RssUpdateListenerProvider.class).to(RssUpdateListenerProviderImpl.class).in(Singleton.class);
        bind(InAppBillingService.class).to(InAppBillingServiceImpl.class).in(Singleton.class);

        bind(Theme.class).in(Singleton.class);
        bind(Settings.class).to(SettingsImpl.class).in(Singleton.class);
        bind(LastModifiedManager.class).in(Singleton.class);
        bind(Authorizer.class).in(Singleton.class);
        bind(EmailSender.class).in(Singleton.class);
        bind(AANHelper.class).in(Singleton.class);
        bind(DriveController.class).to(DriveControllerImpl.class).in(Singleton.class);
        bind(Billing.class).to(BillingImpl.class).in(Singleton.class);

        bind(ImageLoaderHelper.class).in(Singleton.class);
        bind(FigureViewerImageLoaderHelper.class).in(Singleton.class);
        bind(ImageLoader.class).toProvider(ImageLoaderProvider.class);

        bind(CustomBroadcastReceiver.class).to(CustomBroadcastReceiverImpl.class).in(Singleton.class);

        // Controllers
        bind(WebController.class).in(Singleton.class);
        bind(VideoController.class).in(Singleton.class);
        bind(ArticleController.class);
        bind(FeedsController.class);
        bind(ConnectionController.class).in(Singleton.class);

        bind(QuickLinkMenuComponent.class);
    }

    @Provides
    public FeedsInfo getFeedsInfo(Injector injector) {
        final Theme theme = injector.getInstance(Theme.class);
        final Settings settings = injector.getInstance(Settings.class);
        final FeedsInfo feedsInfo = new FeedsInfo();

        feedsInfo.setEarlyViewFeed(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "earlyViewArticleRefs.feed"));
        feedsInfo.setIssueListFeed(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "issueList.feed"));
        feedsInfo.setKeywordsFeed(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "keywords.feed"));
        feedsInfo.setSubscribedKeywordsFeed(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "keywordsInfo.feed?"));
        feedsInfo.setArticleZipPrefix(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "article.zip?doi="));
        feedsInfo.setArticleInfoPrefix(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "articleInfo.feed?doi="));
        feedsInfo.setIssueZipPrefix(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "issue.zip?doi="));
        feedsInfo.setIssueTocPrefix(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "issueToc.feed?doi="));
        feedsInfo.setSpecialSectionPrefix(format("%s/%s",
                theme.getServerUrlOnServer(settings.getCurrentServer()),
                "specialSectionArticles.feed?id="));
        feedsInfo.setInAppContentFeed(theme.getInAppContentFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setSocietyFeed(theme.getSocietyFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setSpecialSectionsListFeed(theme.getSpecialSectionsListFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setTPSFeed(theme.getTPSFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setAdvertisementFeed(theme.getAdvertisementFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setLicenseFeed(theme.getLicenseFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setRegisterNewUserFeed(theme.getRegisterNewUserFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setHomeScreenFeed(theme.getHomeScreenFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setAnnouncementFeed(theme.getAnnouncementsFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setRssContentFeed(theme.getRssContentFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setAffiliationFeed(theme.getAffiliationFeedOnServer(settings.getCurrentServer()));
        feedsInfo.setRestrictedFeed(theme.getRestrictedStatusFeed(settings.getCurrentServer()));
        feedsInfo.setMcsIdentityFeed(theme.getMcsIdentityFeedOnServer(settings.getCurrentServer()));

        return feedsInfo;
    }

    @Provides
    public AnnouncementFeedListener announcementFeedListenerProvider(final Injector injector) {
        final Context context = injector.getInstance(Context.class);
        final AnnouncementFeedListener instance = new AnnouncementFeedListener(DeviceUtils.isPhone(context));
        injector.injectMembers(instance);
        return instance;
    }

    private static class ImageLoaderProvider implements Provider<ImageLoader> {

        protected final ImageLoaderHelper imageLoaderHelper;

        @Inject
        public ImageLoaderProvider(final ImageLoaderHelper imageLoaderHelper) {
            this.imageLoaderHelper = imageLoaderHelper;
        }

        @Override
        public ImageLoader get() {
            return this.imageLoaderHelper.getLoader();
        }
    }

    private static class CachePathProvider implements Provider<String> {

        protected final Context context;

        @Inject
        public CachePathProvider(final Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public String get() {
            return context.getCacheDir().getAbsolutePath();
        }
    }

    private static class ExternalCachePathProvider implements Provider<String> {

        protected final Context context;

        @Inject
        public ExternalCachePathProvider(final Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public String get() {
            final File externalCacheDir = context.getExternalCacheDir();
            return externalCacheDir != null ? externalCacheDir.getAbsolutePath() : "";
        }
    }


    private static class DatabasePathProvider implements Provider<String> {

        protected final String cachePath;

        @Inject
        public DatabasePathProvider(@InjectCachePath String cachePath) {
            this.cachePath = cachePath;
        }

        @Override
        public String get() {
            return FileUtils.joinPath(cachePath, "db", "journalapp.db");
        }
    }
}
