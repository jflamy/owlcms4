package app.owlcms.athlete.data.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.vaadin.artur.helpers.CrudService;

import app.owlcms.athlete.data.entity.Person;

@Service
public class PersonService extends CrudService<Person, Integer> {

    @PersistenceContext
    private EntityManager entityManager;

    private PersonRepository repository;

    public PersonService(@Autowired PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    protected PersonRepository getRepository() {
        return repository;
    }

    public Long getCountJPQL() {
        Query query = entityManager.createQuery("select count(p) from Person p");
        return (long) query.getSingleResult();
    }
    
    @SuppressWarnings("unchecked")
    public List<Person> getAllJPQL() {
        Query query = entityManager.createQuery("select p from Person p");
        return query.getResultList();
    }

    public Long getCountUsingSpecification() {
        Specification<Person> s = (person, q, b) -> b.like(person.get("lastName"), "Tesi");
        System.err.println(repository.findAll(s));
        System.err.println(repository.count(s));
        return repository.count(s);
    }

}
