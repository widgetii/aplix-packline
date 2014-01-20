package ru.aplix.packline.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("file:src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml")
public class HomeControllerTests {

	@Autowired
	private HomeController controller;

	private MockMvc mockMvc;

	@PostConstruct
	public void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	public void testHome() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("home"));
	}

	@Test
	public void testControl() throws Exception {
		mockMvc.perform(post("/ctrl").param("action", "start")).andExpect(status().isMovedTemporarily()).andReturn();
		Thread.sleep(1000);
		mockMvc.perform(post("/ctrl").param("action", "stop")).andExpect(status().isMovedTemporarily()).andReturn();
	}

	@Test
	public void testAddParcel() throws Exception {
		mockMvc.perform(post("/add").param("parcelId", "123456")).andExpect(status().isMovedTemporarily()).andReturn();
	}

	@Test
	public void testRemoveParcel() throws Exception {
		mockMvc.perform(post("/remove").param("parcelId", "123456")).andExpect(status().isMovedTemporarily()).andReturn();
	}
}
