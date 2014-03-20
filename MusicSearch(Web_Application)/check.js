var req = false;
var inputt;
var stype;
var outMsg;


function check(form_data)
{
	var chk= form_data.detail.value;
	if(!chk)
	{
		html = "";
		var print = document.getElementById('result');
 		print.innerHTML = html;
		alert("Please enter something in the search box");
		return false;
	}
	else
    {
		init(form_data);
		return false;
	}
}

function init(form_data)
 {
	inputt = form_data.detail.value;
	
	stype = form_data.stype.value;
	
	var url = "http://cs-server.usc.edu:23825/examples/servlet/HelloWorldExample?input="+inputt+"&type="+stype;
	
	loadXML(url);	
 }
 
 function loadXML(url) 
 {	
 	req = false;
 	
 	if(window.XMLHttpRequest) 
 	{
	     try
	      {   req = new XMLHttpRequest();
 	      
 	      } catch(e)
 	      { 
 	     	  req = false;
 	      }
 	    // branch for IE/Windows ActiveX version
 	} 
 	else if(window.ActiveXObject)
 	{
        try {
 	          req = new ActiveXObject("Msxml2.XMLHTTP");
 	        } catch(e) {
 	          try {  req = new ActiveXObject("Microsoft.XMLHTTP");
 	          } catch(e) {   req = false;
 	          }
 	          }
 	}
 	if(req) 
 	{
 		req.onreadystatechange = myCallBack;
 		req.open("GET", url, true);
 		req.setRequestHeader("Connection", "Close");
 		req.setRequestHeader("Method", "GET" + url + "HTTP/1.1");
 		req.send();
 	}
 	else {
 		document.getElementById("result").innerHTML = "Sorry, but I couldn't create an XMLHttpRequest";
 	}
 }
 
 function myCallBack()
 {
	if (req.readyState == 4) {
		if (req.status == 200) {
			 outMsg = eval('(' + req.responseText + ')');
			printResult();
		}
		else {
			 outMsg = "There was a problem with the request " + req.status;
			document.getElementById("result").innerHTML = outMsg;
		}
		
	}
 }
 
 function printResult()
 {
 	if(stype == "artists")
 	{
 		info = outMsg.results.result;
 		if(outMsg['results'] == "No Result found")
 		{
 			html = "";
 			html += "<html><head></head><body><h2 style=' text-align:center;'> No Result Found </h2></body></html>";
 			var print = document.getElementById('result');
 			print.innerHTML = html;
 		}
 		else
 		{
 		length = info.length - 1;
 		html = "";
 		html += "<html><head></head><body><h2 style=' text-align:center;'> Displaying "+length+" results for "+inputt+" </h2>";
 		html += "<table border='1' style='margin:auto'><br /><tr><th>Cover</th><th>Name</th><th>Genre(s)</th><th>Year(s)</th><th>Details</th><th>Post to Facebook</th></tr>"
 				
 		for(var i=1;i<info.length;i++)
 		{
 			if(info[i].cover == "NA")
 			{
 				var img = "<td><img src=\"zoom.gif\" height=100px width=100px alt=\"NA\" /></td>";	
 				info[i].cover = "http://cs-server.usc.edu:23825/examples/servlets/zoom.gif";
 			}
 			else
 			{
 				var img = "<td><img src="+info[i].cover+" alt=\"NA\" /></td>";	
 			}
 			var name = "<td>"+info[i].name+"</td>";
 			var genre = "<td>"+info[i].genre+"</td>";
 			var year = "<td>"+info[i].year+"</td>";
 			var details = "<td><a href="+info[i].details+" alt=\"NA\">details</a></td>";
 			var post = "<td><input type=\"image\" src=\"facebook.png\" width=100 height=100 margin:0; padding:0 onclick=\"postToFeed("+i+")\"></td>";
 			
 			html += "<tr>"+img+name+genre+year+details+post+"</tr>";
 		}
 		
 		html +="</table></body></html>";
 		var print = document.getElementById('result');
 		print.innerHTML = html;
 		}
 	}
 	else if (stype == "albums")
 	{
 		info = outMsg.results.result;
 		if(outMsg['results'] == "No Result found")
 		{
 			html = "";
 			html += "<html><head></head><body><h2 style=' text-align:center;'> No Result Found </h2></body></html>";
 			var print = document.getElementById('result');
 			print.innerHTML = html;
 		}
 		else
 		{
 			length = info.length - 1;
 			html = "";
 			html += "<html><head></head><body><h2 style=' text-align:center;'> Displaying "+length+" results for "+inputt+" </h2>";
 			
 			html += "<table border='1' style='margin:auto'><br /><tr><th>cover</th><th>Title</th><th>Artist</th><th>Genre(s)</th><th>Year(s)</th><th>Details</th><th>Post to Facebook</th></tr>"		 
 			
 			for(var i=1;i<info.length;i++)
 			{
 				if(info[i].cover == "NA")
 				{
 					var img = "<td><img src=\"zoom.gif\" height=100px width=100px alt=\"NA\" /></td>";
 					info[i].cover = "http://cs-server.usc.edu:23825/examples/servlets/zoom.gif";	
 				}
 				else
 				{
 					var img = "<td><img src="+info[i].cover+" alt=\"NA\" /></td>";	
 				}
 				var title = "<td>"+info[i].title+"</td>";
 				var artist = "<td>"+info[i].artist+"</td>";
 				var genre = "<td>"+info[i].genre+"</td>";
 				var year = "<td>"+info[i].year+"</td>";
 				var details = "<td><a href="+info[i].detail+" alt=\"NA\">details</a></td>";
 				var post = "<td><input type=\"image\" src=\"facebook.png\" width=100 height=100 margin:0; padding:0 onclick=\"postToFeed("+i+")\"></td>";
 				
 				html += "<tr>"+img+title+artist+genre+year+details+post+"</tr>";
 			}
 			html +="</table></body></html>";
 			var print = document.getElementById('result');
 			print.innerHTML = html;
 		}
 			
 	}	
 	else
 	{
 		info = outMsg.results.result;
 		if(outMsg['results'] == "No Result found")
 		{
 			html = "";
 			html += "<html><head></head><body><h2 style=' text-align:center;'> No Result Found </h2></body></html>";
 			var print = document.getElementById('result');
 			print.innerHTML = html;
 		}
 		else
 		{
 			length = info.length - 1;
 			html = "";
 			html += "<html><head></head><body><h2 style=' text-align:center;'> Displaying "+length+" results for "+inputt+" </h2>";
 			
 			html += "<table border='1' style='margin:auto'><br /><tr><th>Sample</th><th>Title</th><th>performer(s)</th><th>composer(s)</th><th>Details</th><th>Post to Facebook</th></tr>"		 
 			
 			for(var i=1;i<info.length;i++)
 			{
 				var sample = "<td><a href="+info[i].sample+" alt=\"NA\"><img src=\"play_button.jpg\" height=80px width=80px /></a></td>";
 				
 				var title = "<td>"+info[i].title+"</td>";
 				var performer = "<td>"+info[i].performer+"</td>";
 				var composer = "<td>"+info[i].composer+"</td>";
 			
 				var detail = "<td><a href="+info[i].detail+" alt=\"NA\">details</a></td>";
 				var post = "<td><input type=\"image\" src=\"facebook.png\" width=100 height=100 margin:0; padding:0 onclick=\"postToFeed("+i+")\"></td>";
 				
 				html += "<tr>"+sample+title+performer+composer+detail+post+"</tr>";
 			}
 			html +="</table></body></html>";
 			var print = document.getElementById('result');
 			print.innerHTML = html;
 		}
 			
 	}
 	
 }
 
 FB.init({ appId: '440238602722748', cookie: true, status: true });

function postToFeed(a) 
{
	if(stype == "artists")
	{
	var obj = {
    	    method: 'feed',
        	link: ''+info[a].details+'',
        	picture: ''+info[a].cover+'',
 		  	name: ''+info[a].name+'',
    	    caption: 'I like '+info[a].name+' who is active since '+ info[a].year,
        	description: 'Genre of Music is: '+info[a].genre,
			properties:{"Look at details:" : {"text":"here","href":info[a].details+""}}
		};
		
		function callback(response)
		{}
		FB.ui(obj, callback);
	}
	else if(stype == "albums")
	{
		var obj = {
    	    method: 'feed',
        	link: ''+info[a].detail+'',
        	picture: ''+info[a].cover+'',
 		  	name: ''+info[a].title+'',
    	    caption: 'I like '+info[a].title+' released in '+ info[a].year,
        	description: 'Artist: '+info[a].artist+' Genre: '+info[a].genre,
			properties:{"Look at details:" : {"text":"here","href":info[a].detail+""}}
		};
		
		function callback(response)
		{}
		FB.ui(obj, callback);
	}
	else
	{
		var obj = {
    	    method: 'feed',
        	link: ''+info[a].detail+'',

 		  	name: ''+info[a].title+'',
    	    caption: 'I like '+info[a].title+' composed by '+ info[a].composer,
        	description: 'Performer: '+info[a].performer,
			properties:{"Look at details:" : {"text":"here","href":info[a].detail+""}}
		};
		
		function callback(response)
		{}
		FB.ui(obj, callback);
	}
}










