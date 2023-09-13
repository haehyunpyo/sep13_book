package com.book.web.login;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

	@Autowired
	private LoginService loginService;

	
	@GetMapping("/login")
	public String login(HttpSession session) {
		// 로그인상태에서 login페이지 이동 막기
		if(session.getAttribute("mid") != null) {
			return "redirect:/";
		}
		return "login";
	}

	
	@GetMapping({"/", "/index"})
	public String getCookie(@CookieValue(name = "setS", required = false) String setS,
							@CookieValue(name = "SuserID", required = false) String SuserID, HttpSession session) {
		
	    if ( setS == null || SuserID == null ) {	// 메인페이지 (setS 쿠키가 없거나 값이 "S"가 아닌 경우)
			return "index";
			
	    } else if(setS.equals("S")) { 				// 자동로그인 진행 (setS 쿠키의 값이 "S"가 아닌 경우)
			//System.out.println(SuserID);
			Map<String, Object> autoresult = loginService.autoLogin(SuserID);	// 쿠키id와 디비id 일치여부확인
			
			if (String.valueOf(autoresult.get("autocount")).equals("1")) {
				
				session.setAttribute("mid", autoresult.get("mid"));
				session.setAttribute("mname", autoresult.get("mname"));
				System.out.println("됨??");
				
			//	return "redirect:/index";
			}
		}
		return "index";
	}
		
	
	@PostMapping("/login")
	public String login(@CookieValue(name = "setS", required = false) String setS,
			@CookieValue(name = "SuserID", required = false) String SuserID,
			@RequestParam Map<String, Object> map, HttpSession session) {

		// if ( setS == null || SuserID == null ) {
			 
				// 일반로그인 진행
				System.out.println(map);
				Map<String, Object> result = loginService.login(map);
				// System.out.println(result);
				if (String.valueOf(result.get("count")).equals("1")) {
					
					session.setAttribute("mid", result.get("mid"));
					session.setAttribute("mname", result.get("mname"));
					//System.out.println(session.getAttribute("mname"));
					
					return "redirect:/booklist";
				}
				
		// } else if ( setS.equals("S") ) {
		//		return "redirect:/booklist";
		//	}
		return "login";
	}

	
	  @ResponseBody
	  @PostMapping("/autoLogin") public String autoCheck(@RequestParam("sid") String sid, HttpSession session) {
	  
		  JSONObject json = new JSONObject();
		  
		  session.setAttribute("mid", sid);
		  System.out.println(session.getAttribute("mid"));
		  
		  int result = loginService.autoCheck(sid);
		  json.put("result", result);
		  System.out.println(result);
	
		  return json.toString(); 
	  }
	 
	  
	  @GetMapping("/logout")
		public String logout(HttpSession session) {
		  
			if(session.getAttribute("mid") != null) {
				session.invalidate();
				System.out.println("로그아웃!!!");
				return "redirect:/login";
			}
			return "redirect:/index";
		}

		@GetMapping("/login/kakao")
		public String kakaoLogin(@RequestParam(required = false) String code, HttpSession session, Model model) {

			// URL에 포함된 code를 이용하여 액세스 토큰 발급
			// System.out.println(code);
			String access_Token = loginService.getKakaoToken(code);
			// System.out.println(access_Token);
			Map<String, Object> kUser = loginService.getKakaoUser(access_Token);  // kmap값을 kUser로 받음.
			// System.out.println(kUser); // {kid=3002751483, kemail=gogus228@hanmail.net}

			// kakao 로그인기록 확인
			int result = loginService.hasKakaoUser(kUser);

			if (kUser != null) { // kakao연결성공

				if (result == 1) {
					// db에 kakao계정정보 있다면 로그인진행
					session.setAttribute("mid", kUser.get("kid"));
					session.setAttribute("withK", "1");	// 로그아웃시 활용
					return "redirect:/";

				} else {
					//db에 kakao계정정보 없다면 생성(id&email) => subjoin에서 진행
					session.setAttribute("mid", kUser.get("kid")); // kid 세션에 저장
					session.setAttribute("withK", "1");	// 로그아웃시 활용
					model.addAttribute("memail", kUser.get("kemail"));
					System.out.println("컨트롤러email값 : " + model.getAttribute("memail"));
					// 위의 mid, memail은 subjoin에 자동기입
					return "subjoin";
				}

			} else {
				return "redirect:login";
			}
		}
	
	
		// 네이버 로그인
		@GetMapping("/login/naver")
		public String naverLogin(@RequestParam(required = false) String code, HttpSession session, Model model) {
			//System.out.println("네이버 가보자고 : " + code);		// code형식 : t9ke0IO0SFl5aNin6F
			String Naccess_Token = loginService.getNaverToken(code);
			
			Map<String, Object> nUser = loginService.getNaverUser(Naccess_Token);
			//System.out.println(NUser);
			// 네이버 로그인기록 확인
			int result = loginService.hasNaverUser(nUser);
			System.out.println(result);	// 0 또는 1
			
			if(nUser != null) {	// 네이버 연결성공
				// db에 naver 계정정보 있다면 로그인진행
				if(result == 1) {
					session.setAttribute("mid", nUser.get("Nid"));
					session.setAttribute("withN", "2");	// 로그아웃시 활용
					return "redirect:/";
					
				} else {	
					// db에 naver계정정보 없다면 생성(id&email&name&phone) => subjoin에서 진행
					session.setAttribute("mid", nUser.get("Nid")); // Nid 세션에 저장
					session.setAttribute("withN", "2");	// 로그아웃시 활용
					// db에 넣을 추가정보들
					model.addAttribute("memail", nUser.get("Nemail"));
					model.addAttribute("mname", nUser.get("Nname"));
					model.addAttribute("mphone", nUser.get("Nphone"));
					return "subjoin";
				}

			}else {
				return "redirect:login";
			}
		}
	

	// 아이디/비번찾기
	@GetMapping("/finduser")
	public String finduser(HttpSession session) {
		// 로그아웃상태에서 finduser페이지 이동 막기
		if(session.getAttribute("mid") != null) {
			return "redirect:/";
		}
		return "finduser";
	}
	
	

}
