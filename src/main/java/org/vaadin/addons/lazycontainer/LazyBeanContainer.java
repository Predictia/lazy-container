package org.vaadin.addons.lazycontainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.filter.SimpleStringFilter;

/**
 * @author Ondrej Kvasnovsky
 */
public class LazyBeanContainer<T> extends BeanContainer<T,T> {

	private static final long serialVersionUID = 5084213735284978664L;

	private SearchCriteria criteria;
    private DAO<T> dao;
    
    private List<OrderByColumn> orderByColumns = new ArrayList<OrderByColumn>();

    // min filter string length, after this length is exceeded database calls are allowed
    private int minFilterLength;

    public LazyBeanContainer(Class<T> type, DAO<T> dao, SearchCriteria criteria) {
        super(type);
        this.criteria = criteria;
        this.dao = dao;
        minFilterLength = 3;
    }

    @Override
    public int size() {
        filterStringToSearchCriteria();
        if (criteria.getLastCount() == 0 || criteria.isDirty()) {
            getCount();
        } else if (isFiltered() && criteria.getFilter() != null) {
            getCount();
        }
        return criteria.getLastCount();
    }

    private void getCount() {
        int count = dao.count(criteria);
        criteria.setDirty(false);
        criteria.setLastCount(count);
    }

    @SuppressWarnings("unchecked")
	@Override
    public BeanItem<T> getItem(Object itemId) {
        return new BeanItem<T>((T) itemId);
    }
    
    private final Map<Integer, T> cachedBeans = new HashMap<Integer, T>();
    
    @Override
    public T getIdByIndex(int idx) {
    	Integer index = idx;
    	if(cachedBeans.containsKey(index)){
    		return cachedBeans.get(index);
    	}
    	return getItemIds(index, 100).get(0);
    }
    
    @Override
    public List<T> getItemIds(int startIndex, int numberOfIds) {
        filterStringToSearchCriteria();
        List<T> items = null;
        if (isFiltered() && criteria.getFilter() != null) {
            items = findItems(startIndex, numberOfIds);
            criteria.setFilter(null);
        } else if (!isFiltered()) {
            items = findItems(startIndex, numberOfIds);
        }
        cachedBeans.clear();
        int i = startIndex;
        for(T item : items){
        	cachedBeans.put(i++, item);
        }
        return items;
    }

    private List<T> findItems(int startIndex, int numberOfIds) {
        List<T> items;
        items = dao.find(criteria, startIndex, numberOfIds, orderByColumns);
        return items;
    }

    private void filterStringToSearchCriteria() {
        if (isFiltered()) {
            Set<Filter> filters = getFilters();
            for (Filter filter : filters) {
                if (filter instanceof SimpleStringFilter) {
                    SimpleStringFilter stringFilter = (SimpleStringFilter) filter;
                    String filterString = stringFilter.getFilterString();
                    if (filterString.length() > minFilterLength) {
                        criteria.setFilter(filterString);
                    } else {
                        criteria.setFilter(null);
                    }
                }
            }
        }
    }

    @Override
    public void sort(Object[] propertyIds, boolean[] ascending) {
        orderByColumns.clear();
        for (int i = 0; i < propertyIds.length; i++) {
            Object propertyId = propertyIds[i];
            OrderByColumn.Type type = ascending[i] ? OrderByColumn.Type.ASC : OrderByColumn.Type.DESC;
            String name = propertyId.toString();
            orderByColumns.add(new OrderByColumn(name, type));
        }
    }

    @Override
    public boolean containsId(Object itemId) {
        // we need this because of value change listener (otherwise selected item event won't be fired)
        return true;
    }

    public int getMinFilterLength() {
        return minFilterLength;
    }

    public void setMinFilterLength(int minFilterLength) {
        this.minFilterLength = minFilterLength;
    }
}
