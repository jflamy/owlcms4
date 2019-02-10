package org.ledocte.owlcms.ui.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "main/test", layout = MainLayout.class)
public class GridTest extends VerticalLayout implements CrudListener<GridTest.Person> {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<Person> people = new ArrayList<>();

    public GridTest() {
        Random random = new Random();
        for (int i = 0; i < 300; i++) {
            people.add(new Person(UUID.randomUUID().toString(), random.nextInt(), i));
        }

        GridCrud<Person> grid = getGrid();
        grid.setSizeFull();

        HorizontalLayout gridWrapper = new HorizontalLayout(grid);
        gridWrapper.setMargin(false);
        gridWrapper.setPadding(false);
        gridWrapper.setSpacing(false);
        gridWrapper.setFlexGrow(1, grid);
        gridWrapper.setSizeFull();


        add(gridWrapper);
        setMargin(false);
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        setFlexGrow(1, gridWrapper);
        setSizeFull();

    }

    private GridCrud<Person> getGrid() {
        GridCrud<Person> grid = new GridCrud<Person>(Person.class);
        grid.setCrudListener(this);
        return grid;
    }

    public class Person {
        private final String name;
        private final int id;
        private final int year;

        public Person(String name, int year, int id) {
            this.name = name;
            this.year = year;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getBirthYear() {
            return year;
        }

        public int getID() {
            return id;
        }
    }

	@Override
	public Collection<Person> findAll() {
		return people;
	}

	@Override
	public Person add(Person domainObjectToAdd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Person update(Person domainObjectToUpdate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Person domainObjectToDelete) {
		// TODO Auto-generated method stub
		
	}

}
