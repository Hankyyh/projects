
$(document).ready(function(){
	$('#content').hide();	
	
	$.ajax({
		url:"AllCelebrityImages.json",
		dataType: "json",
		success:function(data){
			print(data);
		}
	});
	
	
	
});

function chart1(){
var pieData = [
				{
					value: 75,
					color:"yellowgreen"
				},
				{
					value : 25,
					color : "#E0E4CC"
				}
				
			
			];

	var myPie = new Chart(document.getElementById("canvas1").getContext("2d")).Pie(pieData,{
		scaleLabel : "75",
	});
}
function chart2(){


	var barChartData = {
			labels : ["200 Cigarattes","Chasing Amy","Gone Baby Gone","Good Wil Huntin"],
			datasets : [
				{
					fillColor : "#F38630",
					strokeColor : "rgba(220,220,220,1)",
					data : [6.8,12.0,20.3,138.4]
				}
	
			]
			
		}

	var myLine = new Chart(document.getElementById("canvas2").getContext("2d")).Bar(barChartData,{
				
				//Boolean - If we show the scale above the chart data			
				scaleOverlay : true,
				
				//Boolean - If we want to override with a hard coded scale
				scaleOverride : true,
				
				//** Required if scaleOverride is true **
				//Number - The number of steps in a hard coded scale
				scaleSteps : 10,
				//Number - The value jump in the hard coded scale
				scaleStepWidth : 20,
				//Number - The scale starting value
				scaleStartValue : 0
	});

	
	}

function chart3(){
	var barChartData = {
			labels : ["Ben Affleck","Casey Affleck"],
			datasets : [
				{
					fillColor : "#7D4F6D",
					strokeColor : "rgba(220,220,220,1)",
					data : [34,17]
				}
			]
			
		}

	var myLine = new Chart(document.getElementById("canvas3").getContext("2d")).Bar(barChartData);
	}



function print(response){
	html = "";
	$.each(response,function(key,value){
		var obj = value;
		$.each(obj,function(key,value){
			 template = "";
				template += "<div class='col-md-2'>";
				template += "<div class='thumbnail' style='width:150px'>";
				if(value.image=="NoImage"){
					template += "<img src='img/no-available-image.jpeg' alt="+value.name+" class='img-rounded' style='height:150px'>";
				}else{
				template += "<img src='"+value.image+"' alt="+value.name+" class='img-rounded' style='height:150px'>";
				}
				template += "<div class='caption'>";
				template += "<h6 style='font-size:9px'><input class='checkboxes' onclick='search()' type='checkbox'>"+value.name+"</h6>";
				template += "</div></div></div>";
				
				html+=template;
		});
	});

	$('#image-templates').append(html); 
	return;
	
}

function firstSet(value,object){
	
	 $("#front-page").fadeOut(400);
	 
	$("#image1").html(object[0]);
	$("#abs1").html("<h6>Benjamin Géza Affleck-Boldt, better known as Ben Affleck, is an American actor, film director, producer, and screenwriter. He first came to attention for his performances in the Kevin Smith films Mallrats, Chasing Amy, and Dogma.</h6><p><img src='img/rotten.jpeg'/>  97% Good Will Hunting (1997)</p><p><b>Total Gross : </b> $1694</p>");
	$("#image2").html(object[1]);
	$("#abs2").html("<h6>Caleb Casey McGuire Affleck-Boldt, is an American actor and film director. Throughout the 1990s and early 2000s, he played supporting roles in mainstream hits like Good Will Hunting and Ocean's Eleven</h6><p><img src='img/rotten.jpeg'/>  97% Good Will Hunting (1997)</p><p><b>Total Gross : </b> $622.60</p>");
	
	$.ajax({
		url:"http://localhost:8080/LinkedStar/NewsExtraction?first_name="+value[0]+"&second_name="+value[1],
		success:function(response){
			if(response == ""){
				$('.table').append("<h5><b>No News Found</b></h5>");
			}else{
			$('.table').append(response);}
		}
	});
	var ob;

	testing();
//	chart1();
	chart2();
	chart3();
	$('#linkmeter').html("75%");
	$(".caption").remove();
	$("#content").show(); 
}

function secondSet(value,object){
	
	 $("#front-page").fadeOut(400);
	 
	$("#image1").html(object[0]);
	$("#abs1").html("<h6>Born in California, Tom Hanks grew up in what he calls a 'fractured' family. His parents were pioneers in the development of marriage dissolution law in that state, and Tom moved around a lot, living with a succession of step-families.</h6><p><img src='img/rotten.jpeg'/>  100% Return With Honor (1999)</p><p><b>Total Gross : </b> $2764.0M</p>");
	$("#image2").html(object[1]);
	$("#abs2").html("<h6>Blond-haired, blue-eyed with an effervescent personality, Meg Ryan graduated from Bethel high school, Bethel in June 1979. Moving to New York, she attended New York University where she majored in journalism.</h6><p><img src='img/rotten.jpeg'/>  88% When Harry Met Sally (1989)</p><p><b>Total Gross : </b>  $153.4M</p>");
	
	$.ajax({
		url:"http://localhost:8080/LinkedStar/NewsExtraction?first_name="+value[0]+"&second_name="+value[1],
		success:function(response){
			if(response == ""){
				$('.table').append("<h5><b>No News Found</b></h5>");
			}else{
			$('.table').append(response);}
		}
	});
	var ob;

	testing2();
//	chart1();
	chart22();
	chart23();
	$(".caption").remove();
	$('#linkmeter').html("64%");
	$("#content").show(); 
}


function chart22(){


	var barChartData = {
			labels : ["Joe Versus the Volcano","Sleepless in Seattle","You've Got Mail"],
			datasets : [
				{
					fillColor : "#F38630",
					strokeColor : "rgba(220,220,220,1)",
					data : [39.4,126.6,115.8]
				}
	
			]
			
		}

	var myLine = new Chart(document.getElementById("canvas2").getContext("2d")).Bar(barChartData,{
				
				//Boolean - If we show the scale above the chart data			
				scaleOverlay : true,
				
				//Boolean - If we want to override with a hard coded scale
				scaleOverride : true,
				
				//** Required if scaleOverride is true **
				//Number - The number of steps in a hard coded scale
				scaleSteps : 10,
				//Number - The value jump in the hard coded scale
				scaleStepWidth : 20,
				//Number - The scale starting value
				scaleStartValue : 0
	});

	
	}
function chart23(){
	var barChartData = {
			labels : ["Tom Hanks","Meg Ryan"],
			datasets : [
				{
					fillColor : "#7D4F6D",
					strokeColor : "rgba(220,220,220,1)",
					data : [41,29]
				}
			]
			
		}

	var myLine = new Chart(document.getElementById("canvas3").getContext("2d")).Bar(barChartData);
	}

function search(){
	if($(":checkbox:checked").length <= 1){
		
	}
	else if($(":checkbox:checked").length > 2){
		alert("Please select 2 celevrity");
	}else{
		var value = new Array();
		var object = new Array();
		var i=0;
		var j=0;
		$(':checkbox:checked').each(function () {
			 value[i] = $(this).parent().text();
			 object[j] = $(this).parent().parent().parent();
			 
			 i++;
			 j++;
			});
	
		if(value[0]=="Ben Affleck"){
			firstSet(value,object);
		}else{
			secondSet(value,object);
		}
	
	}
}


	
	function testing(){
		var width = 800,
	    height = 800;

	var cluster = d3.layout.cluster()
	    .size([height, width - 220]);

	var diagonal = d3.svg.diagonal()
	    .projection(function(d) { return [d.y, d.x]; });

	var svg = d3.select('.test').append("svg")
	    .attr("width", width)
	    .attr("height", height)
	  .append("g")
	    .attr("transform", "translate(70,0)");

	d3.json("flare.json", function(error, root) {
	  var nodes = cluster.nodes(root),
	      links = cluster.links(nodes);

	  var link = svg.selectAll(".link")
	      .data(links)
	    .enter().append("path")
	      .attr("class", "link")
	      .attr("d", diagonal);

	  var node = svg.selectAll(".node")
	      .data(nodes)
	    .enter().append("g")
	      .attr("class", "node")
	      .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })

	  node.append("circle")
	      .attr("r", 4.5);

	  node.append("text")
	      .attr("dx", function(d) { return d.children ? -8 : 8; })
	      .attr("dy", 3)
	      .style("text-anchor", function(d) { return d.children ? "end" : "start"; })
	      .text(function(d) { return d.name; });
	});

	d3.select(self.frameElement).style("height", height + "px");
		}
	
	
	function testing2(){
		var width = 800,
	    height = 800;

	var cluster = d3.layout.cluster()
	    .size([height, width - 220]);

	var diagonal = d3.svg.diagonal()
	    .projection(function(d) { return [d.y, d.x]; });

	var svg = d3.select('.test').append("svg")
	    .attr("width", width)
	    .attr("height", height)
	  .append("g")
	    .attr("transform", "translate(70,0)");

	d3.json("flare2.json", function(error, root) {
	  var nodes = cluster.nodes(root),
	      links = cluster.links(nodes);

	  var link = svg.selectAll(".link")
	      .data(links)
	    .enter().append("path")
	      .attr("class", "link")
	      .attr("d", diagonal);

	  var node = svg.selectAll(".node")
	      .data(nodes)
	    .enter().append("g")
	      .attr("class", "node")
	      .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })

	  node.append("circle")
	      .attr("r", 4.5);

	  node.append("text")
	      .attr("dx", function(d) { return d.children ? -8 : 8; })
	      .attr("dy", 3)
	      .style("text-anchor", function(d) { return d.children ? "end" : "start"; })
	      .text(function(d) { return d.name; });
	});

	d3.select(self.frameElement).style("height", height + "px");
		}
	