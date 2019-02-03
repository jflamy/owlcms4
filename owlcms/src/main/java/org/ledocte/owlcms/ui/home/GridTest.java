package org.ledocte.owlcms.ui.home;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Route(value = "GridTest", layout = MainLayout.class)
public class GridTest extends VerticalLayout {
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

        Grid<Person> grid = getGrid();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
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

    private Grid<Person> getGrid() {
        return getGrid(people.size());
    }

    private Grid<Person> getGrid(int size) {
        Grid<Person> grid = new Grid<>();
        grid.setItems(people.subList(0, size));
        grid.addColumn(Person::getName).setHeader("Name 1");
        grid.addColumn(Person::getName).setHeader("Name 2");
        grid.addColumn(Person::getName).setHeader("Name 3");
        return grid;
    }


    class Person {
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

}
