package app.owlcms.athlete.data.service;

import org.springframework.data.jpa.repository.JpaRepository;

import app.owlcms.athlete.data.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {

}