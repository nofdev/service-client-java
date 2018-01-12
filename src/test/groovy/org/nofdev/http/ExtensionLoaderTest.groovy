package org.nofdev.http

import org.junit.Test
import org.nofdev.client.filter.DurationTimeFilter
import org.nofdev.client.filter.RpcContextFilter
import org.nofdev.core.Caller
import org.nofdev.core.Request
import org.nofdev.core.RpcFilter
import org.nofdev.extension.ActivationComparator
import org.nofdev.extension.ExtensionLoader

/**
 * Created by Liutengfei on 2018/1/12
 */
class ExtensionLoaderTest {
    @Test
    public void demo() {
        // 返回所有实现类
        println ExtensionLoader.getExtensionLoader(RpcFilter).getExtensions("")
        //返回指定的实现类
        println ExtensionLoader.getExtensionLoader(RpcFilter).getExtension("DurationTimeFilter")
        println ExtensionLoader.getExtensionLoader(RpcFilter).getExtension("rpcContext")
    }

    @Test
    public void aaa() {
        List<RpcFilter> filters = new ArrayList<>()
        filters.add(new DurationTimeFilter())
        filters.add(new RpcContextFilter())
        filters.add(new AFilter())
        filters.add(new ZFilter())
        filters.add(new GFilter())

        Collections.sort(filters, new ActivationComparator<RpcFilter>())

        println filters
    }

    class AFilter implements RpcFilter {

        @Override
        Object invoke(Caller caller, Request request) throws Throwable {
            return null
        }
    }
    class GFilter implements RpcFilter {

        @Override
        Object invoke(Caller caller, Request request) throws Throwable {
            return null
        }
    }

    class ZFilter implements RpcFilter {

        @Override
        Object invoke(Caller caller, Request request) throws Throwable {
            return null
        }
    }
}
