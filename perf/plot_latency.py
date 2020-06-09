import numpy
import matplotlib.pyplot as plt

sites = ['google_com', 'bike_discount_de', 'abhijeet_castings_co_in', 'metro_tokyo_lg_jp', 'webmasters_stackexchange_com', 'doctorofcredit_com']

def load_file(fileName):
    return numpy.loadtxt(fileName)


def load_data(dns_server):
    data = []

    for prefix_A in sites:
        data.append(load_file(prefix_A + '_' + dns_server + '.txt'))

    return data


def main():
    fig, ax = plt.subplots()
    ax.set_title('DNS Query Time')

    x = range(0, len(sites))
    
    dns_servers = ['pdns', 'google_dns', 'comcast_dns']
    styles = ['b-', 'g--', 'r--']
    label_pos = [3, -1, -1]

    boxes = []
    for i in range(0, len(dns_servers)):
        dns_server = dns_servers[i]
        data = load_data(dns_server)
        
        means = []
        for j in x:
           means.append(numpy.median(data[j]))

        if dns_server == 'pdns':
            boxes.append(ax.boxplot(data, positions=x, patch_artist=True, showfliers=False))

        ax.plot(x, means, styles[i], label=dns_server)
    
    colors = ['pink', 'lightblue', 'lightgreen']
    for i in range(0, 1):
        bplot = boxes[i]
        for patch in bplot['boxes']:
            patch.set_facecolor(colors[i])

    ax.yaxis.grid(True)
    ax.set_xlabel('Hops')
    ax.set_ylabel('Query Time (msec)')
    plt.legend()
    plt.show()


if __name__ == "__main__":
    main()
