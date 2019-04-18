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

function addTouchEventsForElement(elem, callback) {
    wwlToucher.onTouch(elem, {
                       ontouch : callback
                       });
}

function addTouchEvents(className, callback) {
    var elements = document.getElementsByClassName(className);
    for (var i = 0; i < elements.length; ++i) {
        addTouchEventsForElement(elements[i], callback);
    }
}

function onBodyClick(e) {
    e.preventDefault();
    
    if (e.target.tagName == 'A') {
        var href = e.target.getAttribute('href');
        window.location.href = href;
    } else {
        var feedItemId = e.currentTarget.parentElement.id;
        window.location.href = "details://" + feedItemId;
    }
    
    return false;
}

function setFeedItemThumbnail (feedItemID, thumbnail) {
    $('#' + feedItemID).find('.feed-thumbnail').html(thumbnail);
}