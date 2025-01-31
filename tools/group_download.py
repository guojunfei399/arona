import os
import yaml
from functools import reduce
from yaml.loader import SafeLoader
from tools import draw_image, draw_image_source
from fetch_student_info_from_ba_game_db import concat_list, concat_two_im

source_map = {
    "1": "巴哈姆特@夜喵貓貓咪喵(asaz5566a)",
    "2": "bilbilibili@朝夕desu(4607471)",
    "3": "bilbilibili@y千代(49533273)",
}

if __name__ == "__main__":
    source = {}
    with open(r"./config/group_download.yml", "r", encoding="UTF-8") as f:
        source = yaml.load(f, SafeLoader)
    count = 1
    images = list(
        filter(lambda item: item['name'] != 'test', list(source['image'])))
    total = len(images)
    concat_group = {}
    for item in images:
        print("%d/%d" % (count, total))
        name = item["name"]
        if name.find(".png") == -1:
            name = name + ".png"
        path = item["path"]
        url = item["url"].split("@")[0]
        local_path = path + name
        if url != "":
            if "source" in item:
                source = item["source"]
                if source in source_map:
                    source = source_map[source]
                draw_image(url, name, path, source=source)
            else:
                draw_image(url, name, path)
        else:
            if not os.path.exists(local_path):
                print("%s: url is empty and local path is not exist. skip" % name)
                count += 1
                continue
        if "group" in item and item["group"] != "":
            group = item["group"]
            
            if "type" in item and item["type"] != "": 
                type = item["type"]
            else:
                type = "horizen"
            if group in concat_group:
                concat_group[group].append({
                    "path": local_path,
                    "type": type
                })
            else:
                concat_group[group] = [{
                    "path": local_path,
                    "type": type
                }]
        count += 1
    if len(concat_group.keys()) != 0:
        def concat(a, b):
            concat_two_im(a["path"], b["path"], a["path"], b["type"], margin=20, reshape=True)
            return { "path": a["path"], "type": b["type"] }
        print("start concat")
        for key in concat_group.keys():
            print("concat group: %s" % key)
            group = concat_group[key]
            reduce(concat, group)
            group.remove(group[0])
            for f in list(map(lambda x: x["path"], group)):
                os.remove(f)