package com.mydomain.clients.backend.apirest.controllers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mydomain.clients.backend.apirest.models.entity.Client;
import com.mydomain.clients.backend.apirest.models.services.IClientSevice;

@CrossOrigin(origins = {"http://localhost:4200"})
@RestController
@RequestMapping("/api")
public class ClientRestController {

	@Autowired
	private IClientSevice clientService;
	
	private final Logger log = LogManager.getLogger(ClientRestController.class);

	@GetMapping("/clients")
	public List<Client> index() {
		return clientService.findAll();
	}
	
	@GetMapping("/clients/page/{page}")
	public Page<Client> index(@PathVariable Integer page) {
		return clientService.findAll(PageRequest.of(page, 4));
	}
	
	@GetMapping("/clients/{id}")
	public ResponseEntity<?> show(@PathVariable Long id) {
		Client client = null;
		Map<String, Object> response = new HashMap<>();
		try {
			client = clientService.findById(id);
		} 
		catch(DataAccessException e) 
		{
			response.put("message", "Error performing the query in the database");
			response.put("error", e.getMessage()+ ": "+ e.getMostSpecificCause().getMessage());
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(client == null) {
			response.put("message", "Client ID: "+id.toString()+" does not exist in the database.");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Client>(client, HttpStatus.OK);
	}
	
	@PostMapping("/clients")
	public ResponseEntity<?> create (@Valid @RequestBody Client client, BindingResult result) {
		Client newClient= null;
		Map<String, Object> response = new HashMap<>();
		
		if(result.hasErrors()) {
			List<String> errors = result.getFieldErrors().stream().map(e-> "The field, '"+e.getField()+"' has the following error: "+e.getDefaultMessage()).collect(Collectors.toList());
			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			newClient =clientService.save(client);
		} 
		catch(DataAccessException e) 
		{
			response.put("message", "Error performing the insert in the database");
			response.put("error", e.getMessage()+ ": "+ e.getMostSpecificCause().getMessage());
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("message", "The client was created successfully");
		response.put("client", newClient);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
		
	}
	
	@PutMapping("/clients/{id}")
	public ResponseEntity<?> update (@Valid @RequestBody Client client, @PathVariable Long id, BindingResult result) {
		Client currentClient = null;
		Client savedClient = null;
		Map<String, Object> response = new HashMap<>();
		
		if(result.hasErrors()) {
			List<String> errors = result.getFieldErrors().stream().map(e-> "The field, '"+e.getField()+"' has the following error: "+e.getDefaultMessage()).collect(Collectors.toList());
			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			currentClient = clientService.findById(id);
		} 
		catch(DataAccessException e) 
		{
			response.put("message", "Error performing the query in the database");
			response.put("error", e.getMessage()+ ": "+ e.getMostSpecificCause().getMessage());
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(currentClient == null) {
			response.put("message", "Error: could not be edited, client ID: "+id.toString()+" does not exist in the database.");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		try {
			currentClient.setName(client.getName());
			currentClient.setLastName(client.getLastName());
			currentClient.setEmail(client.getEmail());
			currentClient.setCreateAt(client.getCreateAt());
			savedClient =clientService.save(currentClient);
		} 
		catch(DataAccessException e) 
		{
			response.put("message", "Error performing the update in the database");
			response.put("error", e.getMessage()+ ": "+ e.getMostSpecificCause().getMessage());
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("message", "The client was updated successfully");
		response.put("client", savedClient);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
		
		
		
	}
	
	@DeleteMapping("/clients/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		Map<String, Object> response = new HashMap<>();
		try {
			Client client = clientService.findById(id);
			
			String lastPhoto = client.getPhoto();
			if(lastPhoto!=null && lastPhoto.length() > 0) {
				Path lastPhotoRoute = Paths.get("uploads").resolve(lastPhoto).toAbsolutePath();
				File lastFile = lastPhotoRoute.toFile();
				if(lastFile.exists() && lastFile.canRead()) {
					lastFile.delete();
				}
			}
			clientService.delete(id);
		} 
		catch(DataAccessException e) 
		{
			response.put("message", "Error performing the delete in the database");
			response.put("error", e.getMessage()+ ": "+ e.getMostSpecificCause().getMessage());
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put("message", "The client was deleted successfully");
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
	}
	
	@PostMapping("/clients/upload")
	public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam Long id) {
		Map<String, Object> response = new HashMap<>();
		Client client = clientService.findById(id);
		
		if(!file.isEmpty()) {
			String fileName = UUID.randomUUID().toString() +"_"+ file.getOriginalFilename().replace(" ", "");
			Path fileRoute = Paths.get("uploads").resolve(fileName).toAbsolutePath();
			log.info(fileRoute.toString());
			try {
				Files.copy(file.getInputStream(), fileRoute);
			} catch (IOException e) {
				response.put("message", "Error performing the file upload");
				response.put("error", e.getMessage()+ ": "+ e.getCause().getMessage());
				return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			String lastPhoto = client.getPhoto();
			if(lastPhoto!=null && lastPhoto.length() > 0) {
				Path lastPhotoRoute = Paths.get("uploads").resolve(lastPhoto).toAbsolutePath();
				File lastFile = lastPhotoRoute.toFile();
				if(lastFile.exists() && lastFile.canRead()) {
					lastFile.delete();
				}
			}
			
			client.setPhoto(fileName);
			clientService.save(client);
			response.put("client", client);
			response.put("message", "The photo was uploaded successfully: "+fileName);
			
		}
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@GetMapping ("/uploads/img/{photoName:.+}")
	public ResponseEntity<Resource> viewPhoto (@PathVariable String photoName){
		Path fileRoute = Paths.get("uploads").resolve(photoName).toAbsolutePath();
		log.info(fileRoute.toString());
		Resource resource = null; 
		try {
			resource = new UrlResource(fileRoute.toUri());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if(!resource.exists() && !resource.isReadable()) {
			throw new RuntimeException("Error, could not load the image: "+photoName);
		}
		
		HttpHeaders headers =  new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename= \"" + resource.getFilename()+"\"");
		
		return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
	}
}
