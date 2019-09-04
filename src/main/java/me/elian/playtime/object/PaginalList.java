package me.elian.playtime.object;

import com.google.common.collect.Lists;
import org.apache.commons.lang.Validate;

import java.util.List;

public class PaginalList<T> {

    private List<List<T>> partitions;
    private int amountPerPage;

    public PaginalList(List<T> list, int amountPerPage) {
        this.amountPerPage = amountPerPage;
        partition(list);
    }

    private void partition(List<T> list) {
        Validate.isTrue(amountPerPage > 0, "Amount per page must be greater than 0");

        partitions = Lists.partition(list, amountPerPage);
    }

    public List<T> getPage(int pageNumber) {
        Validate.isTrue(pageNumber > 0, "Page number must be greater than 0");

        if (partitions.size() < pageNumber)
            throw new IllegalArgumentException("Page number cannot be greater than partition size");

        return partitions.get(--pageNumber);
    }

    public int getAmountOfPages() {
        return partitions.size();
    }
}
