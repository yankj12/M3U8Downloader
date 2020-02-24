# M3U8Downloader
java实现的m3u8视频下载工具

***
## 概述
使用java实现的m3u8视频下载工具
请求视频内容采用的是httpclient+流处理的方式
使用多线程将ts视频下载到文件夹
将多个ts文件合并为一个mp4文件

## 开发设计

以 [关于m3u8格式的视频文件ts转mp4下载和key加密问题](https://www.cnblogs.com/String-Lee/p/11391893.html) 这篇参考资料为主

### m3u8文件概述

m3u8文件中有ts文件的地址、加密METHOD和key的地址

- ts文件的地址，有的是绝对地址，有的是相对地址
- 加密METHOD和key的地址

![m3u8文件示例](https://img2018.cnblogs.com/blog/1518154/201908/1518154-20190821230102828-1548723868.png)

### 保存为html文件，下载ts文件，代码如下：可加多线程，可能需要用代理。

```python
# 爬虫 123.html就是打开m3u8文件右键保存为html格式。
htmlf=open('./123.html','r',encoding="utf-8")
htmlcont=htmlf.read()
# print(htmlcont)
import requests
from lxml import etree
tree = etree.HTML(htmlcont)
href = tree.xpath("//a//@href")
# print(href)
ts = href[2273:]
print(len(ts))
# print(ts)
for i in ts:
    a = i.split("/")[-1]
    # print(a)
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.87 Safari/537.36"}
    try:
        rest = requests.get(i,headers=headers)
        if rest.status_code == 200:
            with open(a,"wb") as fp:
                fp.write(rest.content)
                fp.close()
    except Exception as e:
         print(e)
```

### AES加密

AES 的 mode 就那么几个，你知道是 AES-128 ，试一下不就好了。

```python
#!/usr/bin/env python
# -*- coding: utf8 -*-
from Crypto.Cipher import AES

raw = file('dyVuoO%2BiKIqY%2B3Ebf3CavNpB5RKlXfGtInP31znaGCfYnVkrSsAF46r2hg-1', 'rb').read()
iv = raw[0:16]
data = raw[16:]
key = file('key', 'rb').read()

plain_data = AES.new(key, AES.MODE_CBC, iv).decrypt(data)
file('fuck.mp4', 'wb').write(plain_data)
```

## 参考资料

- [关于m3u8格式的视频文件ts转mp4下载和key加密问题](https://www.cnblogs.com/String-Lee/p/11391893.html)
- [ffmpeg合并m3u8 ts key文件 解决Invalid data found when processing input错误](https://blog.csdn.net/u014484783/article/details/79350392)
- [M3U8、key、和一堆ts的视频，请问怎么合并](https://www.52pojie.cn/thread-685227-1-1.html)
