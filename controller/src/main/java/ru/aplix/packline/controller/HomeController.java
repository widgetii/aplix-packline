package ru.aplix.packline.controller;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {

	@Autowired
	private PostManager postManager;

	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		model.addAttribute("lineState", postManager.isRunning() ? "running" : "stopped");
		model.addAttribute("numParcels", postManager.getNumberOfParcels());

		return "home";
	}

	/**
	 * Controls post manager.
	 */
	@RequestMapping(value = "/ctrl", method = RequestMethod.POST)
	public String control(@RequestParam("action") String action, Locale locale, Model model) {
		if ("start".equalsIgnoreCase(action)) {
			postManager.start();
		} else if ("stop".equalsIgnoreCase(action)) {
			postManager.stop();
		}

		return "redirect:/";
	}

	/**
	 * Adds parcel to queue.
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public String addParcel(@RequestParam("parcelId") String parcelId, Locale locale, Model model) {
		postManager.addParcelToQueue(parcelId);

		return "redirect:/";
	}

	/**
	 * Removes parcel from queue.
	 */
	@RequestMapping(value = "/remove", method = RequestMethod.POST)
	public String removeParcel(@RequestParam("parcelId") String parcelId, Locale locale, Model model) {
		postManager.removeParcelFromQueue(parcelId);

		return "redirect:/";
	}
}
