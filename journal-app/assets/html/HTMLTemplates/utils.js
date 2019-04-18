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

function pos(elm) {
	var test = elm, top = 0, left = 0;
	
	while(!!test && test.tagName.toLowerCase() !== "body") {
		top += test.offsetTop;
		left += test.offsetLeft;
		test = test.offsetParent;
	}
	
	return [left, top];
}

function viewPortHeight() {
	var de = document.documentElement;
	
	if(!!window.innerWidth)
	{ return window.innerHeight; }
	else if( de && !isNaN(de.clientHeight) )
	{ return de.clientHeight; }
	
	return 0;
}

function scrollY() {
	if( window.pageYOffset ) { return window.pageYOffset; }
	return Math.max(document.documentElement.scrollTop, document.body.scrollTop);
}

function scrollX() {
	if( window.pageXOffset ) { return window.pageXOffset; }
	return Math.max(document.documentElement.scrollLeft, document.body.scrollLeft);
}

function getHeadersPosition() {
    var elm = document.querySelectorAll('div.section_heading_class');
    var headers = "{";

    for(var i = 0; i < elm.length; i++) {
        var position = pos(elm[i]);
        if (i > 0) {
            headers += ", ";
        }
        headers += "'" + elm[i].id + "'" + ":" + "'" + position[1] + "'";
    }

    headers += "}";
    return headers;
}

function scrollHeaderToPosition(headerId, offsetTop) {
    $('body').scrollTo('#' + headerId, 0, {offset: offsetTop});
}

function getHeadersInfo() {
        var elm = document.querySelectorAll('div.section_heading_class');
        var headers = "{";

        for(var i = 0; i < elm.length; i++) {
            var position = pos(elm[i]);
            if (i > 0) {
                headers += ", ";
            }

            var headerText = elm[i].textContent.replace(/'/g, "\\'");
            headers += "'" + position[1] +  "'" + ":" + "'" + headerText + "'";
        }

        headers += "}";

        return headers;
}

function getHeadersTitle() {
    var elm = document.getElementsByClassName('section_heading_class');
    var titles = "";
    
    for(var i = 0; i < elm.length; i++) {
        if (elm[i].tagName === 'DIV') {
            titles += $.trim(elm[i].textContent) + "###";
        }
    }
    
    return titles.substring(0, titles.length - 3);
}

function getFirstVisibleSectionId() {
    var scrollTop = $(window).scrollTop();
    var windowHeight = $(window).height();
    var partialyVisible = null;
    var firstVisible = null;
    $('[id^=section_with_id_]').each(
        function() {
            var offset = $(this).offset();
            if(offset.top < scrollTop && $(this).height() + offset.top < (scrollTop + windowHeight)) {
                partialyVisible = $(this);
            }
            if ( offset.top >= scrollTop && offset.top < (scrollTop + windowHeight) && firstVisible == null) {
                firstVisible = $(this);
            }
        }
                              );

    if(firstVisible != null)
        return firstVisible.attr('id').replace('section_with_id_', '');
    else if(partialyVisible != null)
        return partialyVisible.attr('id').replace('section_with_id_', '');
    else
        return "";
}

function getThumbnailsLocation() {
    var thumbnails = $('img:not([src])');
    var json = {};
    
    for(var i = 0; i < thumbnails.length; i++) {
        var thumbnail = thumbnails[i];
        var h = thumbnail.offsetHeight,
            y = pos(thumbnail.parentElement),
            id = $(thumbnail).attr('data-thumbnail');
        
        json[id] = { y: y[1], h: h };
    }
    
    return JSON.stringify(json);
}


function getElementAbsRect(element_id)
{
    var elm = (typeof element_id == "string" ? document.getElementById( element_id ) : element_id),
        w = elm.offsetWidth,
        h = elm.offsetHeight,
	    p = pos(elm);
	
	return "{{" + p[0] + ", " + p[1] + "}, " + "{" + w + ", " + h + "}}";
}

function getElementRect(element_id)
{
	var elm = document.getElementById( element_id ),
	    w = elm.offsetWidth,
	    h = elm.offsetHeight,
	    vpH = viewPortHeight(), // Viewport Height
	    st = scrollY(), // Scroll Top
	    sl = scrollX(),
	    p = pos(elm);
	
	return "{{" + (p[0] - sl) + ", " + (p[1] - st) + "}, " + "{" + w + ", " + h + "}}";
}

function getFirstVisibleElementIDOfClass(element_class)
{
    var scrollTop = $(window).scrollTop();
    var windowHeight = $(window).height();
    var partialyVisible = null;
    var firstVisible = null;
    $("."+element_class).each(
        function() {
            var offset = $(this).offset();
            if(offset.top < scrollTop && $(this).height() + offset.top < (scrollTop + windowHeight)) {
                partialyVisible = $(this);
            }
            if ( offset.top >= scrollTop && offset.top < (scrollTop + windowHeight) && firstVisible == null) {
                firstVisible = $(this);
            }
        }
                              );
    
    if(firstVisible != null)
        return firstVisible.attr('id');
    else if(partialyVisible != null)
        return partialyVisible.attr('id');
    else
        return "";
}

(function(){
	function _onElementTouchStart(event) {
		if (typeof(this.__wwlOnTouchStart) == 'function')
			this.__wwlOnTouchStart.call(this, event);

		this.__wwlTouch = true;
		this.__wwlTouchX = event.changedTouches[0].pageX;
		this.__wwlTouchY = event.changedTouches[0].pageY;
	}

	function _onElementTouchMove(event) {
		if (Math.abs(event.changedTouches[0].pageX - this.__wwlTouchX) > 30 ||
			Math.abs(event.changedTouches[0].pageY - this.__wwlTouchY) > 8) {
			this.__wwlTouch = false;
		}
	}

	function _onElementTouchEnd(event) {
		if (typeof(this.__wwlOnTouchEnd) == 'function')
			this.__wwlOnTouchEnd.call(this, event);

		if (this.__wwlTouch && typeof(this.__wwlOnTouch) == 'function') {
			this.__wwlOnTouch.call(this, event);
		}
	}

	function _addTouchEventsToElement(element, handlers) {
		if (!handlers)
			return;
		var elm = typeof(element) == 'string' ? document.getElementById(element) : element;

        if (elm) {
            if (handlers.ontouch) elm.__wwlOnTouch = handlers.ontouch;
            if (handlers.ontouchend) elm.__wwlOnTouchEnd = handlers.ontouchend;
            if (handlers.ontouchstart) elm.__wwlOnTouchStart = handlers.ontouchstart;

            elm.addEventListener('touchend', _onElementTouchEnd, false);
            elm.addEventListener('touchmove', _onElementTouchMove, false);
            elm.addEventListener('touchstart', _onElementTouchStart, false);
		}
	}

	window.wwlToucher = {
		/// @param: element - can be html element it's id.
		///
		/// @param: onTouch - should be function which will be called 
		/// if no movement will be on element. Event 'ontouchend' will be passed as first argument.
		///
		/// @param: onTouchStart (optional) - should be function which will be called
		/// when will be ontouchstart event on this element. Event will be passed as first argument.
		///
		/// @param: onTouchEnd (optional) - should be function which will be called
		/// when will be ontouchend event on this element. Event will be passed as first argument.
		///
		/// Using example:
		/// 	wwlToucher.onTouch('element_id', {
		///			ontouch: function(){
		///				alert("I'm tapped!");
		///			},
		/// 
		///			// ontouchstart function
		///			ontouchstart: function(event) {
		///				// 'this' is element itself
		///				// highlight element
		///				this.style.background = 'yellow';
		///			},
		/// 
		///			// ontouchend function
		///			ontouchend: function(event) {
		///				// 'this' is element itself
		///				// dehighlight element
		///				this.style.background = 'white';
		///			});
		onTouch: _addTouchEventsToElement
	};

})();

function scrollTableLeft() {
    var $elm = $(this);
    var table = $elm.parents('div.table');
    table.animate({
                  scrollLeft: 0
                  }, 200);
}

function recalcTablesWidth() {
    var articleBody = document.getElementById('article_main');
    var isIPad = hostCallbacks.isTablet();
    var ths = $('#article_main .table thead th');
    var tds = $('#article_main .table tbody td');
    ths.css('white-space', 'nowrap');
    tds.css('white-space', 'nowrap');
    $('#article_main div.table table').each(function(){
        var table = $(this);
        if (!table.is(':visible') || table.is(':hidden')) {
            return;
        }

        table.css('width', 'auto');

        var row = null;
        for (var i = 0; i < this.rows.length; ++i) {
            if (row === null || row.cells.length < this.rows[i].cells.length) {
                row = this.rows[i];
            }
        }

        if (row !== null) {
            var tableWidth = 0;
            table.find('col').remove();
            var maxColWidth = isIPad ? 0.3 : 1;
            if(isIPad && row.cells.length == 1) {
                maxColWidth = 0.9;
            }
            if(isIPad && row.cells.length == 2) {
                maxColWidth = 0.5;
            }
            for (var i = row.cells.length - 1; i >= 0; --i) {
                var width = Math.min(row.cells[i].offsetWidth, articleBody.offsetWidth * maxColWidth);
                tableWidth += width;
                var colHtml = "<col style='width:" + width + "px;'>";
                $(this).prepend(colHtml);
            }
        }
        table.css('width', tableWidth + 'px');
    });
    ths.css('white-space', 'normal');
    tds.css('white-space', 'normal');

    var html_body = $('body');
    var div_tables = $('div.table');
    //
    div_tables.each(function() {
        if(!($(this).parent().hasClass('tWrapper'))) {
            var prev = $(this).prev();
            var parent = $(this).parent();
                    
            var div_tWrapper = $('<div class="tWrapper">');
            var div_tCaption = $('<div class="tCaption">');
            var div_tBody = $('<div class="tBody">');
            var div_tFooter = $('<div class="tFooter">');
            var tag_caption = $(this).find('caption');
            tag_caption.children().each(function() {
                div_tCaption.append(this);
            });
            tag_caption.remove();

            var tag_tfoot_td = $(this).find('tfoot td');
            tag_tfoot_td.children().each(function() {
                div_tFooter.append(this);
            });
            $(this).find('tfoot').remove();

            var tag_tbl = $(this).find('table');

            if (tag_tbl.length > 1) {
                $(this).removeClass('table').addClass('tableGroup');
                var _this = $(this);
                tag_tbl.each(function () {
                    var div_tWrapper_2 = $('<div class="tWrapper">');
                    var div_tTable_2 = $('<div class="table">');
                    var div_tBody_2 = $('<div class="tBody">');

                    div_tBody_2.append($(this));
                    div_tTable_2.append(div_tBody_2);
                    div_tWrapper_2.append(div_tTable_2);
                    _this.append(div_tWrapper_2);
                });
            } else {
                tag_tbl.appendTo(div_tBody);
                div_tBody.appendTo($(this));
            }

            div_tCaption.appendTo(div_tWrapper);
            $(this).appendTo(div_tWrapper);
            div_tFooter.appendTo(div_tWrapper);

            if (prev.length !== 0) {
                div_tWrapper.insertAfter(prev);
            } else {
                parent.prepend(div_tWrapper);
            }
        }
    });

    var div_wrappers = $($('div.tWrapper').get().reverse());
    div_wrappers.each(function() {
        var $this = $(this);
        if (!$this.is(':visible') || $this.is(':hidden') || $this.find('table').length > 1) {
            return;
        }

        var div_tBody = $this.find('.tBody');
        var tag_tbl = $this.find('table');

        div_tBody.find('.table-end-block').remove();
        tag_tbl.css('margin-right', '0px');
      
        var tBodyWidth = div_tBody.outerWidth();
        if (tBodyWidth < articleBody.offsetWidth - 20) {
            $this.css('width', tBodyWidth + 'px');
//            $this.css('margin', '0 auto');
        }

        if ($(articleBody).width() < tBodyWidth) {
            var div_endBlock = $('<div class="table-end-block">');
            var div_endArrow = $('<div class="table-end-arrow">&laquo;</div>');
            div_endBlock.append(div_endArrow);
                      
            div_tBody.append(div_endBlock);
            tag_tbl.css('margin-right','20px');
            
            wwlToucher.onTouch(div_endBlock[0], {
                ontouch: scrollTableLeft
            });
        }
    });

    // MDA-3327 (move tables to left for some cases)
    $('.tWrapper').each(function () {
       if ($(this).find('.tWrapper:not([style])').length) {
           $(this).find('.tWrapper[style]').addClass('tblLeftAlign');
       }
    });
    
    /*
     // Add shadow to wide tables
     if (document.getElementsByClassName('table-arrow-left').length == 0) {
     var tables = document.getElementsByClassName('table');
     for(var i in tables) {
     var table = tables[i];
     if (table.getElementsByTagName('table')[0].offsetWidth > articleBody.offsetWidth) {
     var arrowLeft = document.createElement('div'),
     arrowRight = document.createElement('div');
     
     arrowLeft.setAttribute('class', 'table-arrow-left');
     arrowRight.setAttribute('class', 'table-arrow-right');
     table.arrowRight = arrowRight;
     table.arrowLeft = arrowLeft;
     table.insertBefore(arrowLeft, table.firstChild);
     table.insertBefore(arrowRight, table.firstChild);
     table.addEventListener('touchmove', touchTable);
     table.addEventListener('touchend', touchTable);
     }
     }
     }*/
    
    
    // Append end arrow to wide tables
    /*    var tables = document.getElementsByClassName('table');
     for(var i in tables) {
     var table = tables[i];
     if(table.getElementsByClassName('tWrapper').length == 0) {
     var tWrapper = document.createElement('div'),
     tBody = document.createElement('div');
     tWrapper.setAttribute('class', 'tWrapper');
     tBody.setAttribute('class', 'tBody');
     
     tBody.appendChild(table.firstElementChild);
     tWrapper.appendChild(tBody);
     table.appendChild(tWrapper);
     }
     if (table.getElementsByTagName('table')[0].offsetWidth > articleBody.offsetWidth) {
     var tBody = table.getElementsByClassName('tBody')[0];
     var tWrapper = table.getElementsByClassName('tWrapper')[0];
     
     tBody.style["padding-right"] = "30px";
     tWrapper.style["padding-right"] = "30px";
     
     var endBlock = document.createElement('div'),
     endArrow = document.createElement('div');
     
     endBlock.setAttribute('class', 'table-end-block');
     endArrow.setAttribute('class', 'table-end-arrow');
     endArrow.innerHTML = "&#9001;&#9001;";
     endBlock.appendChild(endArrow);
     table.style["margin-right"] = "0px";
     table.firstElementChild.style["padding-right"] = "40px";
     tBody.appendChild(endBlock);
     }
     }*/
    /*
     if (document.getElementsByClassName('table-end-block').length == 0) {
     var div_tables = document.getElementsByClassName('table');
     for(var i in div_tables) {
     var div_table = div_tables[i];
     
     if (div_table.getElementsByTagName('table')[0].offsetWidth > articleBody.offsetWidth-20) {
     var endBlock = document.createElement('div'),
     endArrow = document.createElement('div');
     
     endBlock.setAttribute('class', 'table-end-block');
     endArrow.setAttribute('class', 'table-end-arrow');
     endArrow.innerHTML = "&#9001;&#9001;";
     endBlock.appendChild(endArrow);
     div_table.style["margin-right"] = "0px";
     div_table.firstElementChild.style["padding-right"] = "30px";
     var div_tBody = div_table.getElementsByClassName('tBody')[0];
     div_tBody.appendChild(endBlock);
     }
     }
     }*/
    
}