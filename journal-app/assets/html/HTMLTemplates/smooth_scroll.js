/*--------------------------------------------------------------------------
 *  Smooth Scroller Script, version 1.0.1
 *  (c) 2007 Dezinerfolio Inc. <midart@gmail.com>
 *
 *  For details, please check the website : http://dezinerfolio.com/
 *
/*--------------------------------------------------------------------------*/

Scroller = {
	// control the speed of the scroller.
	// dont change it here directly, please use Scroller.speed=50;
	speed:10,
	
	// scroll top offset. if ofsset < 0 then element positioned to vertical center of screen;
	// default is 0 - no offset.
	offset:0,

	// returns the Y position of the div
	gy: function (d) {
		var gy = d.offsetTop;
		if (d.offsetParent) while (d = d.offsetParent) gy += d.offsetTop;
		return gy;
	},

	// returns the current scroll position
	scrollTop: function (){
		var body=document.body;
	    var d=document.documentElement;
	    if (body && body.scrollTop) return body.scrollTop;
	    if (d && d.scrollTop) return d.scrollTop;
	    if (window.pageYOffset) return window.pageYOffset;
	    return 0;
	},

	// attach an event for an element
	// (element, type, function)
	add: function(event, body, d) {
	    if (event.addEventListener) return event.addEventListener(body, d,false);
	    if (event.attachEvent) return event.attachEvent('on'+body, d);
	},

	// kill an event of an element
	end: function(e){
		if (window.event) {
			window.event.cancelBubble = true;
			window.event.returnValue = false;
      		return;
    	}
	    if (e.preventDefault && e.stopPropagation) {
			e.preventDefault();
			e.stopPropagation();
	    }
	},
	
	scrollToElement: function(element, onend, toffset){
		Scroller.end(element);
		clearInterval(Scroller.interval);
		Scroller._elementHeight = element.offsetHeight;
		Scroller.interval=setInterval(function(){
			Scroller.scroll(Scroller.gy(element), onend, toffset);
		}, 30);
	},
	
	// move the scroll bar to the particular div.
	scroll: function(d, endfunc, toffset){
		var i = window.innerHeight || document.documentElement.clientHeight;
		var h = document.body.scrollHeight;
		var a = Scroller.scrollTop();
		var o = toffset ? toffset : Scroller.offset;
		if (o < 0)
			o = Math.max(0, Math.ceil((i - Scroller._elementHeight) / 2));
		d -= o;
		if(d>a)
			if(h-d>i)
				a+=Math.ceil((d-a)/Scroller.speed);
			else
				a+=Math.ceil((d-a-(i-(h-d)))/Scroller.speed);
		else
			a = a+(d-a)/Scroller.speed;
		window.scrollTo(0,a);
	  	if(a==d || Scroller.offsetTop==a)
		{
			clearInterval(Scroller.interval);
			if (endfunc)
				endfunc();
		}
		Scroller.offsetTop=a;
	},
}


/*------------------------------------------------------------
 *						END OF CODE
/*-----------------------------------------------------------*/