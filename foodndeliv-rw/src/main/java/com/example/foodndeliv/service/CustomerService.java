package com.example.foodndeliv.service;

import com.example.foodndeliv.repository.*;
import com.example.foodndeliv.types.CustomerState;
import com.example.foodndeliv.dto.*;
import com.example.foodndeliv.entity.*;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;


@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    Keycloak keycloak;

    //Get application properties
    @Value("${keycloak.target-realm}")
    private String realm;

    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO customerRequestDTO) {
        Customer newCustomer = new Customer();

        Optional<Customer> customeropt = customerRepository.findByName(customerRequestDTO.getName());

        if(customeropt.isPresent())
        {
                throw new RuntimeException("Customer exists");
        }
        
        newCustomer.setName(customerRequestDTO.getName());
        newCustomer.setEmail(customerRequestDTO.getEmail());
        newCustomer.setState(CustomerState.ACTIVE);


        Customer savedCustomer = customerRepository.save(newCustomer);
        

        //Create in KeyCloak, deleting the newly created customer on failure, and throw a RuntimeException
        // Based on https://github.com/phuongtailtranminh/Keycloak-Admin-Client-Spring-Boot-Demo/blob/master/src/main/java/me/phuongtm/KeycloakService.java
        // + https://gist.github.com/thomasdarimont/c4e739c5a319cf78a4cff3b87173a84b
        //+https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/admin/client/package-summary.html

        //Derive Realm and User Resources from keycloak admin client bean
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        //User details
        UserRepresentation user = new UserRepresentation();
        user.setUsername(savedCustomer.getName());
        user.setEmail(savedCustomer.getEmail());
        user.setEnabled(true);

        //credentials
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setTemporary(true);
        credential.setValue("blablabla");
        user.setCredentials(List.of(credential));

        //User details submit to keycloak
        usersResource.create(user);

        // Fetch the user and assign customer role - ASSUMES IT HAS BEEN SETUP
        String userId = usersResource.search(savedCustomer.getName()).get(0).getId();
        RolesResource rolesResource = realmResource.roles();
        usersResource.get(userId).roles().realmLevel().add(List.of(rolesResource.get("customer").toRepresentation()));

        // Set a custom attribute
        user.singleAttribute("custid", savedCustomer.getId().toString());
        usersResource.get(userId).update(user);

        return modelMapper.map(savedCustomer, CustomerResponseDTO.class);
    }

    @Transactional
    public CustomerResponseDTO updateCustomer(Long id, CustomerUpdateDTO customerUpdateDTO) {

        Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

        CustomerState prevCustomerState = customer.getState();

        customer.setEmail(customerUpdateDTO.getEmail());
        customer.setState(customerUpdateDTO.getState());

        //Update in KeyCloak based on state, throwing a RuntimeException on failure
        // Based on https://github.com/phuongtailtranminh/Keycloak-Admin-Client-Spring-Boot-Demo/blob/master/src/main/java/me/phuongtm/KeycloakService.java
        // + https://gist.github.com/thomasdarimont/c4e739c5a319cf78a4cff3b87173a84b
        //+https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/admin/client/package-summary.html


        if(customer.getState()!= CustomerState.ACTIVE){ // updating state to BLOCKED OR INACTIVE

            System.out.println("Deactivating customer");

            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
    
            String userId = usersResource.search(customer.getName()).get(0).getId();
            UserRepresentation user = usersResource.get(userId).toRepresentation();
            user.setEnabled(false); 
            usersResource.get(userId).update(user);

        } else if ((prevCustomerState == CustomerState.INACTIVE) && (customer.getState()== CustomerState.ACTIVE)) { //Reactivating (from INACTIVE) ONLY IN CASE SESSION HAS NOT EXPIRED!!

            System.out.println("Reactivating customer");

            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
    
            String userId = usersResource.search(customer.getName()).get(0).getId();
            UserRepresentation user = usersResource.get(userId).toRepresentation();
            user.setEnabled(true); 
            usersResource.get(userId).update(user);

        } //Unblocking requires a completely different endpoint invokable only by an admin

        Customer savedCustomer = customerRepository.save(customer);
       
        return modelMapper.map(savedCustomer, CustomerResponseDTO.class);

    }

}
