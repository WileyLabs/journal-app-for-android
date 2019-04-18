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
package com.wiley.wol.client.android.data.xml.loader.asset;

import com.wiley.wol.client.android.data.xml.loader.ArticleLoader;
import com.wiley.wol.client.android.domain.DOI;
import com.wiley.wol.client.android.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssetBasedArticleLoader extends AbstractAssetBasedFeedLoader implements ArticleLoader {

    private static final String ASSETS_PREFIX = "assets://";
    private static final String ARTICLE_DATA_BASE_PATH = "offline_data/articles/";
    private static final String ARTICLE_IMAGE_M_FOLDER = "image_m";
    private static final String ARTICLE_IMAGE_N_FOLDER = "image_n";
    private static final String ARTICLE_EQUATION_FOLDER = "equation";
    private static final String ARTICLE_XML = "article.xml";

    @Override
    public InputStream load(final DOI doi) throws IOException {
        final String filePathName = ARTICLE_DATA_BASE_PATH + getArticleFolderName(doi) + "/" + ARTICLE_XML;
        return doLoadInternal(filePathName);
    }

    @Override
    public String getArticleFolderName(final DOI doi) {
        return "article_" + doi.getAssetCompatibleValue();
    }

    @Override
    public List<String> getArticleImagesPaths(final DOI doi) {
        final List<String> imageM = getBitmapsPathsFromArticleFolder(doi, ARTICLE_IMAGE_M_FOLDER);
        final List<String> imageN = getBitmapsPathsFromArticleFolder(doi, ARTICLE_IMAGE_N_FOLDER);
        final List<String> equations = getBitmapsPathsFromArticleFolder(doi, ARTICLE_EQUATION_FOLDER);
        imageM.addAll(imageN);
        imageM.addAll(equations);
        return imageM;
    }

    private List<String> getBitmapsPathsFromArticleFolder(final DOI doi, final String bitmapsFolder) {
        final List<String> result = new ArrayList<String>();
        try {
            final String pathToImages = ARTICLE_DATA_BASE_PATH + getArticleFolderName(doi)
                    + "/" + bitmapsFolder;
            final String[] imagesList = getAssetManager().list(pathToImages);
            for (final String image : imagesList) {
                result.add(ASSETS_PREFIX + pathToImages + "/" + image);
            }
        } catch (Exception e) {
            Logger.d("AssetBasedArticleLoader", e.getMessage());
        }
        return result;
    }

}
