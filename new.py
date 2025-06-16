import csv
import requests
import time


def trans_date(v_timestamp):
    """10位时间戳转换为时间字符串"""
    v_timestamp = float(v_timestamp)
    timeArray = time.localtime(v_timestamp)
    otherStyleTime = time.strftime("%Y-%m-%d %H:%M:%S", timeArray)
    return otherStyleTime


def tran_gender(gender_tag):
    """转换性别"""
    if gender_tag == 1:
        return '男'
    elif gender_tag == 0:
        return '女'
    else:  # -1
        return '未知'


def fecth_everything(comment):
    gender = tran_gender(comment["author"]["member"]["gender"])
    # 评论的时间
    time_un = comment["created_time"]
    publish_time = trans_date(time_un)
    # 评论的IP属地
    IP = comment["address_text"]
    # 评论的内容
    content = comment["content"]
    # 评论的点赞数
    num = comment["vote_count"]

    return gender, publish_time, IP, content, num


# TODO:输入 answer 号
answer_id = input("请输入 answer 号：")
# TODO:添加你的 User-Agent
ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
# TODO:添加你的 cokkie
c = "_xsrf=D96wynzDXi7BKT9ptP6J8nDHgeZRXaox; _zap=ba3ac0b6-ad60-4484-9da6-b2ac259fb918; d_c0=0uTTooIKOxqPTr4PAxyawkh_LYnaLGEk3II=|1743418776; Hm_lvt_98beee57fd2ef70ccdd5ca52b9740c49=1743418782; __zse_ck=004_LoSBFp4Ne2sKxEdTmA=h/lIJb0dA1N1nh=gAOHYLjQ0i5DXCBXWH25=PJyblh9rTWZFUKaZOV/4qNF9dodMpNDLicxpjRem9Momawje=Y505o9f2pvJj83WmU0BDSAM7-efSO4b3+hnBk8m9yq5v+Co9rfYKRrTuYFCjlDatqlmHBxtY9MfCVYFQ8GocHckWTH887rE6m9jhPEC/ojvI+bisI0XCsxuseD6LNrq+wdQh9qDqd6JyCB1c+qA/VNF+y; SESSIONID=WLYVoitvGXEDLIyjeiu3i7qri5DzeO0uwp9wB3jLONJ; JOID=WlsUB0uuCi0-cofwcG39d__3gchj8kRxbBXlhgPhWWp_OsaCGLLSYlpxjvdxOQ95-jo7npca_mk1OQAEqGhB_Bo=; osd=UV0XAU6lDC44d4z2c2v4fPn0h81o9Ed3aR7jhQXkUmx8PMOJHrHUZ1F3jfF0Mgl6_D8wmJQc-2IzOgYBo25C-h8=; DATE=1749999381661; __snaker__id=3gm7UOMKotslxnQz; crystal=U2FsdGVkX19AA5YTsVCXrKBzaWyNnc8ip0wa8jXJr0fF6p+QolmEkzjQbPfIsm1dRNRyMYT7kBTyVxeYLEeb1V0AhO9SD6fQWP6Osgen3kUNtut8f4gk76FPFSJdWB95eei+TSdXZPzHBt7cmEZK9xp94HkS5fduvALheXhIw/6nILGx522MXtYPTnQKe5AEjXjX0qosi31Nptucov5o+bGvxfPNs5aeR1xCfx9DGKvNzHRTgLiQOFrirmZsIq8W; vmce9xdq=U2FsdGVkX183PjAplXrbWaXH7ORTFjb44IoNvfdFZTYiUqIg3NNy+yZbRClA+EWR36WftwHTM6iPM8bWEuA4fuVqricb21EJ8ZUvsigFzNYpJCh2/hQuE/jbNbRxPy89CgLEcNZ+jnVcP6LyoTlR0w==; cmci9xde=U2FsdGVkX1+3cXoXOxR5YCntKPjps6Bs9KKde2OcjuFCpM6WGDZXgH/uSwuvBReKG7zIyvWdEybYv0RiHfZ36g==; pmck9xge=U2FsdGVkX19npjj9ZmzfG6pYQFP+LD6R6+AfkHYSAI0=; assva6=U2FsdGVkX1+RPRu5YU3ggCMBhH8rdzDm1XWLL/qhPNA=; assva5=U2FsdGVkX1+w8arCYE2ScLM1a5M+6mMbUUoU46gObvFL/uLJ5yLDtvjxFzg+dRluR2Ad2v9ppHBD8ekaZckilg==; o_act=login; ref_source=other_https://www.zhihu.com/signin; expire_in=15552000; q_c1=41ec1b62b0a94d5daa233224a4df04f5|1749999407000|1749999407000; gdxidpyhxdE=%2FZ3i3eR6ZztrjPL%2FO6u3sIjhR3dshplS3To0n%2FYZLAH%5ClchHKxk2DKQ2hraR2uJxwvKpihlQ75B%2F7KhHSSA9rIdgEyWfZ%5CqC6sebUOyZRj3QaL1PmUYXeG5UJbC46D8SN4aBGpsPbCswQV0zPpd%2B4x2uPwu7oW4Yz43CDeQBCjONQ6Rq%3A1750003090425; captcha_session_v2=2|1:0|10:1750002201|18:captcha_session_v2|88:ZzZsc2tZVTdOb2V2MVNWUkhMejRuM2dzU1hUN1pLQ1pIMktMdWpqTnR1bEFzMzRqa3dvUDBpN1VPM1RLWDYvYQ==|982ea7b4efca93459b4f795199a80d2219d0a8b51df79c2cdfdfd231efcce616; captcha_ticket_v2=2|1:0|10:1750002211|17:captcha_ticket_v2|728:eyJ2YWxpZGF0ZSI6IkNOMzFfdFVoaVNIbTVMRHBBLkJzam1hZm1VWWx4RGVXMy4qenZFdi5LQ1FuSFdBbEIwazNZNGsycUJtQ3JXenlsS1lMaXN3bWs4NWJlZGJPX2ZHTENRS2hjWExYQXRYVHVCVk1TenVPOXd1bUp6aDJkck5YT0l0V2YxenVOcmx4YW85eWxUcipmKjBFNkRaaHdORVp4aURjMU1IbkZEbV9Id0x3a01UMHR4OXJFQTg5ajBuZHpfSS5PaWJ1UnUwYmhsRnoxNWVWU2NSRTY4ZFM5VWZuSGFUYXUqZUNsc1puMk0wYkJoWGFzYWRETkVTQTRyTlQ2cEJLNmVkSGNEMEpyV1JHd1AudUNKSCpnSWV5WWZQNUVUb2FIMlVHM08xMDV6WTFzNFhSYWxUMEludmxNSXdqR1NfY3J3czZmVkc5RlRWQWxJVl8wMjIwZXBCMURoRUlJamZrejVqSDkuaXEuYXFXOG81UTVfUS5IWWJCYlNsMSpQMnFQUlJTWUpIUzVWYlZMWXRTZmI2Q09RM1hpOEk1WTZGZUhMd2pST1VsTVViczhHRWxJKndneE11WFBxT2lmZnF0QjFSQTZfLmlLc3pTankuYURodlB5dkVGYnVTdkYwamZuUVhPeEVrbVZoY1NTdFlmdGU0Slp5NVQzQUVBQ1NtUDFHMjJtVF9ISEEqU0M1d2RmVFk3N192X2lfMSJ9|006d0968ed3acc98fb8e0accd3eace5aafa5cccfd759125023aa6699d548ff1a; z_c0=2|1:0|10:1750002211|4:z_c0|92:Mi4xXzhOSldRQUFBQURTNU5PaWdnbzdHaVlBQUFCZ0FsVk5Jemc4YVFBbFZCVnhtXzdCQ01haE9Kdk0yQWJZLWFpMHFR|c0dc47b60529144c41f7f24c358584df5320c5136afcbcbb784c05534eecfe51; tst=r; BEC=69a31c4b51f80d1feefe6d6caeac6056"
# 定义参数
headers = {
    'user-agent': ua
}
cookies = {
    # 填自己的z_0 cookie
    'cookie': c
}

# 加载 csv 文件
f = open("comments.csv", mode="a", encoding="utf-8-sig")
csv_write = csv.writer(f)

# 开始爬取
i = 0
j = 1
offset = 0
while i == 0:
    # 获取数据
    url = f"https://www.zhihu.com/api/v4/answers/{answer_id}/root_comments?order=normal&limit=20&offset={offset}&status=open"
    resp = requests.get(url, headers=headers, cookies=cookies)
    data = resp.json()
    # 无数据时终止循环
    if data["data"] == None:
        break
    print(f"正在爬取第{j}页!")
    comments = data["data"]
    for comment in comments:
        # 获取信息
        gender, publish_time, IP, content, num = fecth_everything(comment)
        # 存文件
        csv_write.writerow([gender, publish_time, IP, content, num])

        # 读取子评论
        if comment["child_comments"]:
            for child in comment["child_comments"]:
                gender, publish_time, IP, content, num = fecth_everything(child)
                # 存文件
                csv_write.writerow([gender, publish_time, IP, content, num])

    # 读取下一个 url
    offset += 20
    j += 1
    # 终止读取
    if offset == 1000:
        break

f.close()
print("Over!")