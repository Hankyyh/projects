<?php
header('Content-type:text/xml');
$input = $_GET['input'];
$input = utf8_decode($input);
$type = $_GET['type'];
$input=str_replace(" ", "%2B", $input);


$url = "http://www.allmusic.com/search/{$type}/{$input}";

$html = file_get_contents($url);

type_check($type,$html);
function changetoxml($vari)
{
	$vari = str_replace("\"","&quot;",$vari);
	return $vari;
}

function type_check($type,$html)
{
	switch($type)
	{
		case 'artists':
		$Artists=preg_split('~<tr class=\"search-result artist\">~',$html);		//contails substrings 
		$count = count(preg_split('~<tr class=\"search-result artist\">~',$html));	//keeping track of count
		
		if($count>6)	//to limit count to 5
		{
			$count=6;
		}
		$cont=1;
		
		if($count > 1)
		{
		$finalString = "<results>";
		while($cont!=$count)		//running loop
		{
// 		========================================IMAGE====================================
		
		$splitting_image = preg_split('~<div class=\"right-of-image\">~',$Artists[$cont]);
		
		$imgChk=preg_match('~img src=\"(.)*~',$splitting_image[0],$match);
		if($imgChk==0)
		{
			$xml_image = "\"NA\"";
		}
		else
		{
			preg_match('~\"http://(.)+com\"~',$match[0],$image_list);
			$xml_image = $image_list[0];

		}


//		============================NAME=============================================

		$split = preg_split('~div class=\"right-of-image\"~',$Artists[$cont]);
		$name_chk = preg_match('~a href=(.)*[>](.)*</a>~',$split[1],$name);
		if($name_chk == 0)
		{
			$artistName="NA";
		}
		else
		{
			$artist_name = preg_split('~(<|>)~',$name[0]);
			$artist_name[1] == trim($artist_name[1]);
			if(strlen($artist_name[1]) == 0)
			{
				$artistName="NA";
			}
			else
			{
			$artistName = $artist_name[1]; 
			}
		}

		
//		==========================GENRE==============================================

		$split2 = preg_split('~div class=\"info\">~',$split[1]);
		
		$split3 = preg_split('~(<|>)~',$split2[1]);

		$split3[0] = trim($split3[0]);
		if(strlen($split3[0]) == 0)		//******CORRECTION NEEDED******//
		{
			$genre = "NA";
		}
		else
		{
			$genre = $split3[0];
		}

		
		$split3[2] = trim($split3[2]);
		if(strlen($split3[2]) == 0)
		{
			$year = "NA";
		}
		else
		{
			$year = $split3[2];
		}
		
//		==========================DETAIL=============================================		
		preg_match('~a href=\"(.)*\"~',$split[0],$a_detail);

		$split4 = preg_split('~\"~',$a_detail[0]);
			
		$artist_detail = "http://www.allmusic.com{$split4[1]}";
		
		$cont +=1;
		
		$genre = str_replace("\"","&quot;",$genre);
		
		$finalString = $finalString."<result cover={$xml_image} name=\"{$artistName}\" genre=\"{$genre}\" year=\"{$year}\" details=\"{$artist_detail}\" />";
		
		
		}
		$finalString = $finalString."</results>";
		$finalString=str_replace("&", "&amp;", $finalString);
		echo ($finalString);

		}
		else
		{
			echo ("<results></results>");
		}
		break;
		
	//*********************************************************************************	**************	
	
	
		case 'albums':
		$Albums=preg_split('~<tr class=\"search-result album\">~',$html);		//contails substrings 
		$count = count(preg_split('~<tr class=\"search-result album\">~',$html));	//keeping track of count
		
		if($count>6)	//to limit count to 5
		{
			$count=6;
		}
		$cont=1;
		
		if($count > 1)
		{
		$album_final = "<results>";
		
		while($cont!=$count)		//running loop
		{
// 		========================================IMAGE====================================
		
		$split = preg_split('~<div class=\"artist\">~',$Albums[$cont]);

		$imgChk=preg_match('~img src=\"(.)*~',$split[0],$match);
		if($imgChk==0)
		{
			$album_image = "\"NA\"";
		}
		else
		{
			preg_match('~\"http://(.)+com\"~',$match[0],$image_list);
			$album_image = $image_list[0];
		}
		
	
//		============================TITLE=============================================


		$name_chk = preg_match('~[<]a title=(.)*~',$split[0],$name);	//check condition????

		
		$split1 = preg_split('~\"~',$name[0]);
		$album_title = $split1[1];	//title
		
		$album_title = str_replace("\"","&quot;",$album_title);
		
		$link = "http://www.allmusic.com{$split1[3]}";
//		===========================Artist===========================================

		$split2 = preg_split('~div class=\"info\">~',$split[1]);
		
		
		$chking = preg_match_all('~<a href=\"http:\/\/(.)*>(.)*</a>~',$split2[0],$artists);
		
		if($chking == 0)
		{
			
			$inner_check = preg_match('~(.)+</div>~',$split2[0],$check);
			if($inner_check == 0)
			{
				$artistName="NA";
			}
			else
			{
				$artistName = $check[0];
			}
		}
		else
		{
		$finally_artist = "";
		

		if(strlen($artists[0][0]) == 0)
		{
			$artistName="NA";
		}
		else
		{
			$chk_count = count(preg_split('~[ ]\/[ ]~',$artists[0][0]));
			$pre_artist = preg_split('~[ ]\/[ ]~',$artists[0][0]);
			if($chk_count == 1)
			{
				$final_artist = preg_split('~(<|>)~',$artists[0][0]);
				$artistName = $final_artist[2];
			}
			else
			{
				for($i = 0; $i < $chk_count; $i++) 
				{
					$final_artist = preg_split('~(<|>)~',$pre_artist[$i]);
					$finally_artist .= "{$final_artist[2]}";
					if($i < $chk_count-1)
					{
						$finally_artist .= "/";
					}
				} 
				$artistName = $finally_artist;
				$finally_artist = "";
			}		
		}
		}
		$artistName = str_replace("\"","&quot;",$artistName);
		
		
	

//		============================GENRE============================================


		
		$split3 = preg_split('~(<|>)~',$split2[1]);
		$split3[0]=trim($split3[0]);
		if(strlen($split3[0]) == 0)
		{
			$year = "NA";
		}
		else
		{
			$year = $split3[0];
		}
		
		$split3[2]=trim($split3[2]);

		if(strlen($split3[2]) == 0)
		{
			$genre = "NA";
		}
		else
		{
			$genre = $split3[2];
		}
		$genre = str_replace("\"","&quot;",$genre);
		
//		=======================DETAILS==============================================

		
		$cont++;
		$album_title = str_replace("\"","&quot;",$album_title);
		$artistName = str_replace("\"","&quot;",$artistName);
		$artistName = str_replace("</div>","",$artistName);
		
		$album_final .= "<result cover={$album_image} title=\"{$album_title}\" artist=\"{$artistName}\" genre=\"{$genre}\" year=\"{$year}\" detail=\"{$link}\"/>";

		}
		$album_final .= "</results>";
		$album_final = str_replace("&", "&amp;", $album_final);
		echo $album_final;
		}
		else
		{
			echo ("<results></results>");
		}
		break;
		
//		****************************************************************************************

		case 'songs':
		$Songs=preg_split('~<tr class=\"search-result song\">~',$html);		//contails substrings 
		$count = count(preg_split('~<tr class=\"search-result song\">~',$html));	//keeping track of count
		
		if($count>6)	//to limit count to 5
		{
			$count=6;
		}
		$cont=1;
		
		if($count > 1)
		{
		$song_final = "<results>";

		while($cont!=$count)		//running loop
		{
		$split = preg_split('~<div class=\"title\">~',$Songs[$cont]);


		$check_counter = preg_match('~a href=(.)*~',$split[0],$match);

		if($check_counter == 0)
		{
			$sample = "NA";
		}
		else
		{
		$split1 = preg_split('~\"~',$match[0]);

			$sample = $split1[1];
		}
		
		
//		==============================================================================
	
		$split2 = preg_split('~<div class=\"info\">~',$split[1]);
		
		$split2_1 = preg_split('~\"~',$split2[0]);
		$link_song = $split2_1[1];
		
		$slice = preg_split('~&quot;~',$split2_1[2]);		//title
		
		$song_title = $slice[1];
		
		$confirm = preg_match('~span(.)*~',$split2[0],$match2);
		if($confirm == 0)
		{
			$song_performer = "NA";
		}
		else
		{
		
		preg_match_all('~<a href=\"http:\/\/(.)*>(.)*</a>~',$match2[0],$performer);

		$finally_performer = "";
		

		if(strlen($performer[0][0]) == 0)
		{
			$song_performer = "NA";
		}
		else
		{
			$chk_count2 = count(preg_split('~[ ]\/[ ]~',$performer[0][0]));
			$pre_performer = preg_split('~[ ]\/[ ]~',$performer[0][0]);
			if($chk_count2 == 1)
			{
				$final_performer = preg_split('~(<|>)~',$performer[0][0]);
				$song_performer = $final_performer[2];
			}
			else
			{
				for($i2 = 0; $i2 < $chk_count2; $i2++) 
				{
					$final_performer = preg_split('~(<|>)~',$pre_performer[$i2]);
					$finally_performer .= "{$final_performer[2]}";
					
					if($i2 < $chk_count2-1)
					{
						$finally_performer .= "/";
					}
				} 
				$song_performer = $finally_performer;
				$finally_performer = "";
			}		
		}
		}

// 		===========================================================================
		

		$chking = preg_match_all('~<a href=\"http:\/\/(.)*>(.)*</a>~',$split2[1],$composer);
		
		if($chking == 0)
		{
			$song_composer = "NA";
		}
		else
		{
		$finally_composer = "";
		

		if(strlen($composer[0][0]) == 0)
		{
			$song_composer = "NA";
		}
		else
		{
			$chk_count = count(preg_split('~[ ]\/[ ]~',$composer[0][0]));
			$pre_composer = preg_split('~[ ]\/[ ]~',$composer[0][0]);
			if($chk_count == 1)
			{
				$final_composer = preg_split('~(<|>)~',$composer[0][0]);
				$song_composer = $final_composer[2];
			}
			else
			{
				for($i = 0; $i < $chk_count; $i++) 
				{
					$final_composer = preg_split('~(<|>)~',$pre_composer[$i]);
					$finally_composer .= "{$final_composer[2]}";
					if($i < $chk_count-1)
					{
						$finally_composer .= "/";
					}
				} 
				$song_composer = $finally_composer;
				$finally_composer = "";
			}		
		}
		}
		
		
		$cont++;
// 		changetoxml($song_composer);
		$song_title = str_replace("\"","&quot;",$song_title);
		$song_performer = str_replace("\"","&quot;",$song_performer);
		$song_composer = str_replace("\"","&quot;",$song_composer);
		$song_final .= "<result sample=\"{$sample}\" title=\"{$song_title}\" performer=\"{$song_performer}\" composer=\"{$song_composer}\" detail=\"{$link_song}\" />";
		
		}
		$song_final .= "</results>";
		$song_final = str_replace("&", "&amp;", $song_final);
		echo ($song_final);
		}
		else
		{
			echo ("<results></results>");
		}
		break;
			
	}
}	

?>
