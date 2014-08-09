/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.*;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.magnum.dataup.VideoSvcApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoSvcUp {
	
	// in memory list of videos
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	private static final AtomicLong currentId = new AtomicLong(0L);
	private VideoFileManager videoDataMgr;
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		checkAndSetId(v);
		long videoId = v.getId();
		videos.put(videoId, v);
		v.setDataUrl(getDataUrl(videoId));
		return v;
	}
	
	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
				@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
				@RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile data,
				HttpServletResponse response) throws IOException
	{

		if (!videos.containsKey(id)) {
			System.out.println("ID Not found");
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} else {
			
			Video v = videos.get(id);
		//System.out.println("Vishal's video id: " + v.getId() + " url: " + v.getDataUrl()
		//		+ " title:" + v.getTitle() + " Content type:" + v.getContentType() +
		//		 " Subject:" + v.getSubject());
			videoDataMgr = VideoFileManager.get();	//singleton
			if (videoDataMgr.hasVideoData(v)) {
				System.out.println("Notice: Overwriting video at id: " + v.getId());
			}
			videoDataMgr.saveVideoData(v, data.getInputStream());
		}
		return new VideoStatus(VideoState.READY);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public @ResponseBody void getVideoData(
			@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			HttpServletResponse response) throws IOException
	{
		if (!videos.containsKey(id)) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} else {
		
		Video v = videos.get(id);
		videoDataMgr.copyVideoData(v, response.getOutputStream());
		}
	}
	
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
	
    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }
	
 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
 	
}
