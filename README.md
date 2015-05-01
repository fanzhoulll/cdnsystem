# Welcome to CDN system

* [Project website](http://david.choffnes.com/classes/cs4700sp15/project5.php)

Introduction
---------------


In this project we implement a CDN system. There are three components of this system:

1. DNS server, responsible for looking for the IP address for client. Notice that in this project our DNS server would only be react to the query that has been specified (-n). DNS query for other hosts would simply be ignored.

2. HTTP servers will be responsible for provide content that client ask for. All content were fetched from a original server, which (in our project) locate in Virginia. We implement a caching policy to reduce the frequency that HTTP server has to ask for content from original server.

3. CDN system, it will dynamically choose the best server for certain client. A challenge here is how to always choose the “best” server in dynamically changing network. Notice that in our implementation the CDN system is integrated in DNS server, since it can be viewed as a performance enhancement to the original DNS server.

Install and Usage
-----------------

We provide scripts to automatically deploy, run and stop the DNS servers and HTTP servers. The usage of script is as followings:

`./[deploy|run|stop]CDN -p <port> -o <origin> -n <name> -u <username> -i <keyfile>`

* -p: port number used by DNS server and HTTP servers (they must be runed on the same port)
* -o: address of your original server
* -n: the host name DNS server would response
* -u: the account name of your replica server
* -i: the path of your ssh private key file (to login into your replica server)

Notice that in scripts you have to identify the host address of your DNS server and replica server

Of course you can always try to deploy your DNS server and HTTP server manually. The right way to do it is to copy the program to whereever you want and run `make`. To run DNS server you should use:

`./dnsserver -p <port> -n <name>`

To run HTTP server you should use:

`./httpserver -p <port> -o <origin>`

High level approach and challenge 
--------------------------------

Since the HTTP server and DNS has nothing unique here, we only briefly talk about how we implement the caching policy and CDN mapping algorithm

1 Cache policy 

We download the statistic data of all the view number of each page on wikipedia. The size of data is too big to be put in the program. After analyze we found that most of the view count is only 1 or even 0. Clearly there is no need to include every view record with the system. So we only pick the former 13589 records. We believe this number is large enough for our relative small size of cache.

Whenever a HTTP fetch some new content from original server, it first check if it has already been put into the server. If it is then HTTP server directly provide this content to client, otherwise HTTP server will make judgement of whether to put this content in cache based on its view record and whether the cache has been full.

If the cache has not been full (after put this content), the HTTP server will put it in the cache whatsoever. Otherwise cache will check what is the lowest number of view for all files in cache, the cache file with lowest view will be deleted. To optimized performance we maintain a hash map in the HTTP server. Whenever we put a new page in the cache we will record its view record in hashmap. This guarantee the high efficiency of checking any page’s view record. 

We also did other optimization like check the host address of page to avoid fetching the wrong page. For example there are two pages, both name index.html, but on host in ece.neu.edu, other host in ccs.neu.edu. Our system can check where does this page come from before return it back to client. 

2 CDN policy

The technique we used here is active measurement. The main idea is like this: whenever a new client comes, since DNS server has no history record of this client, it will always choose the closest server to this client. After that it will notify all the servers to measure their latency to this server and return this information. The DNS will compare the information it got from each server and choose the best one (lowest latency) and record in a DNS cache. Next time when this client comes again the CDN system can get the record out of cache and return back the best server to the client. 

However many other questions have also to be considered when designing this system:

(1) What is the real latency of user fetching a page from HTTP server?

It is actually decided by two factors: the delay from user to HTTP server and the delay between HTTP server to original server. So whenever DNS want HTTP server to measure its delay with the HTTP client what it actually measured is the total delay from HTTP server to original server and HTTP server to HTTP client. 

A difficulty here is that we are unable to ping original server directly, the ICMP packet seems to be blocked by the server. The approach we used is using TCP probe to test the latency between HTTP server and original server. The idea is to asking for a page that does not exist in the original server, and original server will reply back a 404 page. The size of this page is identical to every HTTP server, so even if it tooks some time to transmit this page, this will not influence the comparison of delay of each server. We measure the delay from HTTP server sends out get message and the time it receive reply and use that as an estimation of RTT. We would like to point out that this estimated delay is very close to the delay measure by ping.

(2) What if the networking condition has changed?

With the varying condition in the network, it is highly possible that original good server will be not good anymore. It is essential to include some mechanism to update DNS cache in this case. The approach here is actually very straightforward: using TTL. Whenever we keep one record in the DNS cache, we also record the time when it is put in the cache. Then next time when the client comes again we check the difference between now time and the time when it is recorded. If it is larger than TTL we will let DNS server to get the latency information from HTTP servers again. But to guarantee that user can always get shortest latency, the searching for delay information is done after the IP address has been returned. Also we keep a unique signal in the record when it is being changing. This is to avoid the another client comes shortly and re ask the DNS to fetch HTTP server for delay information. 


There are many other challenges, like how to maintain the DNS cache, how to provide multithread support, but we think the above two are the most important challenges in this project.


Test

Many test are done, we only listed some of  them here:

1. Performance test of DNS server and HTTP server
2. Performance test of CDN system, whether it can always reply with best server
3. Performance of multi threading support, we write a multithread dig and wget to test whether the system can handle the multithread requests.
4. Performance test of scripts, whether can we correctly deploy, run and stop the whole system.

