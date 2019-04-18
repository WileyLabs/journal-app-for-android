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

function onOpenOnWOLTouchStart() {
    $(this).removeClass("onOpenOnWOLTouchEnd");
    $(this).addClass("onOpenOnWOLTouchStart");
    onOpenOnWOLTouch();
}

function onOpenOnWOLTouchEnd() {
    $(this).removeClass("onOpenOnWOLTouchStart");
    $(this).addClass("onOpenOnWOLTouchEnd");
}

document.addEventListener('DOMContentLoaded', onDocumentReady, false);

function onDocumentReady(event) {
    var keyword_buttons = $('.keyword-btn');

    keyword_buttons.each(function(index, button) {
        var switchery = new Switchery(button);
        $(button).on('change', onSwitcheryChange);
    });
    
    wwlToucher.onTouch("open_on_wol", {
                       ontouchend   : onOpenOnWOLTouchEnd,
                       ontouchstart : onOpenOnWOLTouchStart
                       });
}

function onSwitcheryChange(e) {
    var keywordId = $(this).attr('id'),
        protocol = this.checked ? 'subscribe://' : 'unsubscribe://';

    window.location.href = protocol + keywordId;
}

function getKeywordTitleById(id) {
    return $('#' + id).data('keyword');
}

function revertBackKeywordStatus(keywordId) {
    var button = $('#' + keywordId);
    if (button.length > 0) {
        button.off('change');
        button.click();
        button.on('change', onSwitcheryChange);
    }
}

//function changeKeywordStatus(jsonString) {
//    var keyword = JSON.parse(jsonString),
//    encodedKeyword = encodeURIComponent(keyword.keyword).replace(/%20/g,'+'),
//    $el = $('#' + keyword.id + ' a'),
//    href = {
//        'subscribed' : 'unsubscribe://' + encodedKeyword,
//        'unsubscribed' : 'subscribe://' + encodedKeyword
//    };
//    
//    if ($el.length > 0) {
//        $el.removeClass('subscribed unsubscribed')
//        .addClass(keyword.styleClass)
//        .text(keyword.text)
//        .attr('href', href[keyword.styleClass]);
//    }
//}
