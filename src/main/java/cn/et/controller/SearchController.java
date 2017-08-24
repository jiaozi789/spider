package cn.et.controller;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.et.index.LuceneUtils;
import cn.et.spilder.Arcticle;
import cn.et.spilder.BaiduSpilder;

@RestController
public class SearchController {
	//Ë÷ÒýÄ¿Â¼
	public final static String indexDir="F:/myindex/";
	@RequestMapping("search")
	public List<Arcticle> search(String key,HttpServletResponse rsp) throws Exception{
		rsp.setHeader("Content-Type", "application/json;charset=UTF-8");
		return LuceneUtils.search(indexDir, key);
	}
	
	@RequestMapping("searchMovie")
	public List<Map> searchMovie(String key,HttpServletResponse rsp) throws Exception{
		String url="http://so.iqiyi.com/so/q_"+URLEncoder.encode(key, "UTF-8");
		Document doc=BaiduSpilder.getDoc(url);
		System.out.println(doc.toString());
		List map=BaiduSpilder.getMovie(doc);
		return map;
	}
}
