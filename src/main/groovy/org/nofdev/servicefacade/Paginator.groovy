package org.nofdev.servicefacade
/**
 * 该类是当查询歌曲时用于分页展示的页面导航器
 *
 * @author Li Hongzhen
 */
class Paginator {
    /**
     * 默认条目数
     */
    static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * 默认第一页的页码
     */
    static final int DEFAULT_FIRST_PAGE = 1;
    /**
     * 每页显示多少条记录
     */
    int pageSize = Paginator.DEFAULT_PAGE_SIZE;
    /**
     * 当前页码
     */
    long page = Paginator.DEFAULT_FIRST_PAGE;


    int getPageSize() {
        return pageSize;
    }

    void setPageSize(int pageSize) {
        this.pageSize = pageSize < 1 ? 1 : pageSize;
    }

    long getPage() {
        return page;
    }

    void setPage(long page) {
        this.page = page;
    }

    long offset() {
        return (page - 1) * pageSize;
    }

    static Paginator page(long page) {
        Paginator paginator = new Paginator();
        paginator.setPage(page);
        return paginator;
    }

    static Paginator page(long page, int pageSize) {
        Paginator paginator = new Paginator();
        paginator.setPage(page);
        paginator.setPageSize(pageSize);
        return paginator;
    }
}
