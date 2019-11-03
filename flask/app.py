from flask import Flask, render_template

app = Flask(__name__)


@app.route('/')
@app.route('/index.html')
def run_index():
    return render_template('index.html')


@app.route('/schools.html')
def run_schools():
    return render_template('schools.html')


@app.route('/schools/<school>')
def run_school(school):
    return render_template('school.html', school_name=school)


if __name__ == '__main__':
    app.debug = True
    app.run(host='0.0.0.0', port=8080)
