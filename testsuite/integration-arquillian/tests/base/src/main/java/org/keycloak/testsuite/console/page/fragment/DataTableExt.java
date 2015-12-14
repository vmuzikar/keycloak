package org.keycloak.testsuite.console.page.fragment;

import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class DataTableExt<DataRepresentationType> extends DataTable {
    protected String editLabel = "Edit";
    protected String deleteLabel = "Delete";
    protected String addLabel = "Add";

    protected abstract DataRepresentationType dataRepresentationFactory();
    protected abstract String getRepresentationId(DataRepresentationType representation);
    protected abstract void setRepresentationId(String value, DataRepresentationType representation);

    public String editLabel() {
        return editLabel;
    }

    public String deleteLabel() {
        return deleteLabel;
    }

    public String addLabel() {
        return addLabel;
    }

    public void clickItem(DataRepresentationType representation) {
        clickItem(representation, false);
    }

    public void clickItem(String id) {
        clickItem(id, false);
    }

    public void clickItem(DataRepresentationType representation, boolean searchFirst) {
        clickItem(getRepresentationId(representation), searchFirst);
    }

    public void clickItem(String id, boolean searchFirst) {
        if (searchFirst) {search(id);}
        waitForBody();
        body().findElement(linkText(id)).click();
    }

    public void clickItemActionButton(DataRepresentationType representation, String buttonLabel) {
        clickItemActionButton(representation, buttonLabel, false);
    }

    public void clickItemActionButton(String id, String buttonLabel) {
        clickItemActionButton(id, buttonLabel, false);
    }

    public void clickItemActionButton(DataRepresentationType representation, String buttonLabel, boolean searchFirst) {
        clickItemActionButton(getRepresentationId(representation), buttonLabel, searchFirst);
    }

    public void clickItemActionButton(String id, String buttonLabel, boolean searchFirst) {
        if (searchFirst) {search(id);}
        waitForBody();
        clickRowActionButton(getRowByLinkText(id), buttonLabel);
    }

    public List<DataRepresentationType> getItemsFromRows() {
        List<DataRepresentationType> rows = new ArrayList<>();
        for (WebElement row : rows()) {
            DataRepresentationType representation = getItemFromRow(row);
            if (representation != null) {
                rows.add(representation);
            }
        }
        return rows;
    }

    public DataRepresentationType getItemFromRow(WebElement row) {
        if (!row.isDisplayed()) {return null;}
        return getItemFromTds(row.findElements(tagName("td")));
    }

    protected DataRepresentationType getItemFromTds(List<WebElement> tds) {
        DataRepresentationType representation = dataRepresentationFactory();
        setRepresentationId(tds.get(0).getText(), representation);
        return representation;
    }

    public List<DataRepresentationType> searchItems(String searchPattern) {
        search(searchPattern);
        return getItemsFromRows();
    }

    public DataRepresentationType findItem(String id) {
        List<DataRepresentationType> items = searchItems(id);
        if (items.isEmpty()) {return null;}
        assert items.size() == 1;
        return items.get(0);
    }
}
