package org.eu.polarexpress.conductor.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class HomeView extends VerticalLayout {

    public HomeView() {
        VerticalLayout layout = new VerticalLayout();
        TextField taskField = new TextField();
        Button addButton = new Button("Add");
        addButton.addClickListener(event -> {
            Checkbox checkbox = new Checkbox(taskField.getValue());
            layout.add(checkbox);
        });
        addButton.addClickShortcut(Key.ENTER);

        add(
                new H1("PolarExpress"),
                layout,
                new HorizontalLayout(
                        taskField,
                        addButton
                )
        );
    }

}
